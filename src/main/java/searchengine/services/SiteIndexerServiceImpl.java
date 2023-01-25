package searchengine.services;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.statistics.*;
import searchengine.exceptions.IndexingException;
import searchengine.model.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Service
public class SiteIndexerServiceImpl implements SiteIndexerService
{
    @Setter
    @Getter
    private volatile boolean isIndexingStarted;
    @Setter
    private volatile boolean isIndexingStopped;
    private final LemmaIndexerService lemmaIndexer;
    private final SiteService siteService;
    private final IndexingConfig indexingConfig;
    private final PageService pageService;
    private Set<Integer> indexingSiteIds = ConcurrentHashMap.newKeySet();
    private Indexer defaultIndexer = new Indexer();
    private ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    private final Logger log = LogManager.getLogger(SiteIndexerServiceImpl.class.getName());

    @Autowired
    public SiteIndexerServiceImpl(SiteService siteService, IndexingConfig indexingConfig,
                                  LemmaIndexerService lemmaIndexer, PageService pageService)
    {
        this.siteService = siteService;
        this.indexingConfig = indexingConfig;
        this.lemmaIndexer = lemmaIndexer;
        this.pageService = pageService;
    }

    synchronized public boolean isStartIndexingPossibility()
    {
        if (isIndexingStarted) throw new IndexingException("Индексация уже запущена.",
                "attempt to start indexing failed, reason: indexing is already started", HttpStatus.FORBIDDEN);

        setIndexingStarted(true);
        return true;
    }

    @Async
    public void startIndexing()
    {
        try
        {
            indexingAllSites();
        }
        finally
        {
            setIndexingStarted(false);
            setIndexingStopped(false);
            indexingSiteIds.clear();
        }
    }

    private void indexingAllSites()
    {
        deleteAllSites();
        List<Site> sites = getAllSitesFromConfig();

        List<Callable<Boolean>> indexingTasks = getIndexingTasks(sites);

        runStatusTimeRefresher();

        forkJoinPool.invokeAll(indexingTasks);
    }

    private void deleteAllSites()
    {
        lemmaIndexer.deleteAll();
        pageService.deleteAllInBatch();
        siteService.deleteAllInBatch();
    }

    private List<Site> getAllSitesFromConfig()
    {
        return indexingConfig.getSites()
                .stream()
                .map(s -> {
                    String siteUrl = getValidUrlFormat(s.getUrl());

                    Site site = new Site();
                    site.setStatus(Status.INDEXING);
                    site.setStatusTime(LocalDateTime.now());
                    site.setName(s.getName());
                    site.setUrl(siteUrl);
                    siteService.save(site);
                    return site;
                }).toList();
    }

    private void runStatusTimeRefresher()
    {
        Thread thread = new Thread(() -> {
            try
            {
                while (!indexingSiteIds.isEmpty())
                {
                    siteService.updateAllStatusTime(indexingSiteIds, LocalDateTime.now());
                    Thread.sleep(10_000);
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private List<Callable<Boolean>> getIndexingTasks(List<Site> sites)
    {
        List<Callable<Boolean>> indexingTasks = new ArrayList<>();

        sites.forEach(site ->
        {
            PageInfo info = indexingPage(site, site.getUrl(), "/");
            Page mainPage = info != null ? info.getPage() : null;

            if (!determineSiteLastError(site, mainPage))
            {
                indexingSiteIds.add(site.getId());
                List<ForkJoinParser> siteIndexersTasks = new ArrayList<>();
                Indexer indexer = new Indexer(site);
                indexer.addUrl(getValidUrlFormat(site.getUrl()));

                siteIndexersTasks.addAll(info.getChildLinks().stream().filter(link -> indexer.isCorrectUrl(link, site.getUrl()))
                        .map(link -> new ForkJoinParser(indexer, link)).toList());

                indexingTasks.add(getSingleSiteIndexingTask(indexer, siteIndexersTasks));

            }
        });
        return indexingTasks;
    }

    private PageInfo indexingPage(Site site, String pageUrl, String urlForSaving)
    {
        return defaultIndexer.indexing(site, pageUrl, urlForSaving);
    }

    private Callable<Boolean> getSingleSiteIndexingTask(Indexer indexer, List<ForkJoinParser> tasks)
    {
        return () -> {
            Integer siteId = indexer.getSite().getId();
            indexingSiteIds.add(siteId);
            ForkJoinParser.invokeAll(tasks);
            indexingSiteIds.remove(siteId);

            if(indexer.getStatus() != Status.FAILED) indexer.setStatus(Status.INDEXED);
            indexer.getSite().setStatusTime(LocalDateTime.now());
            siteService.save(indexer.getSite());
            indexer.clear();
            return true;
        };
    }

    private boolean determineSiteLastError(Site site, Page mainPage)
    {
        String siteError = getErrorMessage(mainPage);
        site.setLastError(siteError);

        if(siteError.equals("")) return false;

        site.setStatus(Status.FAILED);
        siteService.save(site);

        return true;
    }

    private String getErrorMessage(Page page)
    {
        String defaultError = "Ошибка индексации: не удалось проиндексировать сайт " +
                "проверьте корректность введенных данных.";
        if (page != null)
        {
            int statusCode = page.getCode();

            if (statusCode == 200)
            {
                return "";
            }

            if (statusCode >= 300 && statusCode < 400)
            {
                return "Ошибка индексации: url запрашиваемого ресурса возможно был " +
                        "временно(постоянно) изменён, проверьте корректность введенных данных.";
            }
            else
            {
                return switch (statusCode)
                        {
                            case 400, 403 -> "Ошибка индексации: отказ в доступе со стороны сайта.";
                            case 404 -> "Ошибка индексации: главная страница сайта не найдена.";
                            case 500 -> "Ошибка индексации: ошибка сервера со стороны сайта.";
                            default -> defaultError;
                        };
            }
        }
        return defaultError;
    }

    private String getValidUrlFormat(String url)
    {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    synchronized public boolean stopIndexing()
    {
        if (!isIndexingStarted) throw new IndexingException("Индексация не запущена",
                "attempt to stop indexing failed, reason: indexing not started", HttpStatus.FORBIDDEN);

        setIndexingStopped(true);
        log.info("The user has stopped indexing.");
        return true;
    }

    private String getValidPageUrlForSaving(String url, int rootUrlLength)
    {
        return url.endsWith("/") ?
                url.substring(rootUrlLength, url.length() -1) : url.substring(rootUrlLength);
    }

    public boolean indexingUserInputPage(String pageUrl)
    {
        Page page = null;
        SiteConfig siteFromConfig = getSiteForReindexingPage(pageUrl);

        Site site = getSiteFromDB(siteFromConfig.getUrl());
        String validPageUrl = getValidPageUrlForSaving(pageUrl, site.getUrl().length());

        Optional<Page> optionalPage = pageService.findByPathAndSite(validPageUrl, site);

        optionalPage.ifPresent(this::deletePageForReindexing);
        PageInfo info = indexingPage(site, pageUrl, validPageUrl);

        if(info == null)
        {
            String error = getErrorMessage(page);
            throw new IndexingException(error, String.format("request to index page(%s) by user failed",pageUrl),
                    HttpStatus.BAD_REQUEST);
        }
        return true;
    }

    private SiteConfig getSiteForReindexingPage(String pageUrl)
    {
        return indexingConfig.getSites()
                .stream()
                .filter(site -> pageUrl.contains(site.getUrl())).findAny().orElseThrow(() ->
                        new IndexingException("Данная страница находится за пределами сайтов, " +
                                "указанных в конфигурационном файле",
                                String.format("request to index a page(%s) outside of sites config file", pageUrl),
                                HttpStatus.BAD_REQUEST)
                );
    }

    private void deletePageForReindexing(Page page)
    {
        pageService.pageLemmasFrequencyDecrement(page);
        pageService.deleteById(page.getId());
    }

    private Site getSiteFromDB(String siteUrl)
    {
        return siteService
                .findByUrlAndStatus(getValidUrlFormat(siteUrl), Status.INDEXED)
                .orElseThrow(() -> new IndexingException("Ошибка: сайт с введенной страницей не проиндексирован.",
                        "request to index a page of a non-indexed site.", HttpStatus.FORBIDDEN));
    }

    @NoArgsConstructor
    public class Indexer
        {
        private Set<String> uniqueUrls = ConcurrentHashMap.newKeySet();
        int siteUrlLength;
        @Getter
        Site site;

        Indexer(Site site)
        {
            this.site = site;
            siteUrlLength = site.getUrl().length();
        }

        public String getUrlForSaving(String url)
        {
            return getValidPageUrlForSaving(url, siteUrlLength);
        }

        public PageInfo indexing(Site site, String pageUrl, String urlForSaving)
        {
            try
            {
                Connection.Response response = connectionToUrl(pageUrl);

                if (!response.contentType().contains("text/html")) return null;

                return getPageInfo(response, site, urlForSaving);
            }
            catch (IOException e)
            {
                log.debug("Error when trying to connect to URI during indexing.", e);
            }
            return null;
        }

        private Connection.Response connectionToUrl(String url) throws IOException
        {
            return Jsoup.connect(url)
                    .userAgent(indexingConfig.getUserAgent())
                    .referrer(indexingConfig.getReferrer())
                    .ignoreContentType(true)
                    .execute();
        }

        private PageInfo getPageInfo(Connection.Response response, Site site, String urlForSaving)
                throws IOException
        {

            int statusCode = response.statusCode();

            Page page = new Page();
            page.setPath(urlForSaving);
            page.setCode(statusCode);
            page.setSite(site);

            if(statusCode == 200)
            {
                Document doc = response.parse();
                page.setContent(doc.outerHtml());
                pageService.save(page);

                startLemmaIndexing(site, page, doc);

                PageInfo info = getPageInfoFromHtml(doc, page);

                urlForSaving = null;
                response = null;
                doc = null;

                return info;
            }
            else
            {
                page.setContent("");
                pageService.save(page);
            }
            return null;
        }

        private void startLemmaIndexing(Site site, Page page, Document html)
        {
            Map<Lemma, Float> lemmas = lemmaIndexer.getLemmas(site, page, html);
            synchronized (this)
            {
                lemmaIndexer.saveNewLemmas(lemmas);
            }
            synchronized (this)
            {
                lemmaIndexer.saveIndexesByLemma(lemmas, page);
            }
        }

        private PageInfo getPageInfoFromHtml(Document html, Page page)
        {
            if(html != null)
            {
                List<String> childLinks = html
                        .select("a")
                        .eachAttr("abs:href");

                return new PageInfo(page, childLinks);
            }

            return null;
        }

        public boolean isCorrectUrl(String pageUrl, String rootUrl)
        {
            pageUrl = getValidUrlFormat(pageUrl);
            return pageUrl.startsWith(rootUrl)
                    && !pageUrl.contains("#")
                    && !pageUrl.contains("?")
                    && !pageUrl.matches(".+(?i)(?:.jpg/?|.png/?|.pdf/?)$")
                    && addUrl(pageUrl);
        }

        public boolean checkIsIndexingStopped()
        {
            if(!isIndexingStopped) return false;

            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем.");
            return true;
        }

        public Status getStatus()
        {
            return site.getStatus();
        }

        public void setStatus(Status status)
        {
            site.setStatus(status);
        }

        public void clear()
        {
            uniqueUrls.clear();
        }

        public boolean addUrl(String pageUrl)
        {
            return uniqueUrls.add(pageUrl);
        }
    }
}