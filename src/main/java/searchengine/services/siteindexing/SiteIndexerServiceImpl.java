package searchengine.services.siteindexing;

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
import searchengine.dto.indexing.PageInfo;
import searchengine.exceptions.IndexingException;
import searchengine.model.*;
import searchengine.services.lemmaindexing.LemmaIndexerService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Service
public class SiteIndexerServiceImpl implements SiteIndexerService {

    @Setter
    @Getter
    private volatile boolean isIndexingStarted;
    @Setter
    private volatile boolean isIndexingStopped;
    private final LemmaIndexerService lemmaIndexer;
    private final SiteService siteService;
    private final IndexingConfig indexingConfig;
    private final PageService pageService;
    private final Set<Integer> indexingSiteIds = ConcurrentHashMap.newKeySet();
    private final Indexer defaultIndexer = new Indexer();
    private final ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
    private final Logger log = LogManager.getLogger(SiteIndexerServiceImpl.class.getName());

    @Autowired
    public SiteIndexerServiceImpl(SiteService siteService, IndexingConfig indexingConfig,
                                  LemmaIndexerService lemmaIndexer, PageService pageService) {
        this.siteService = siteService;
        this.indexingConfig = indexingConfig;
        this.lemmaIndexer = lemmaIndexer;
        this.pageService = pageService;
    }

    synchronized public boolean isStartIndexingPossibility() {
        if (isIndexingStarted) throw new IndexingException("???????????????????? ?????? ????????????????.",
                "attempt to start indexing failed, reason: indexing is already started", HttpStatus.FORBIDDEN);

        setIndexingStarted(true);
        return true;
    }

    @Async
    public void startIndexing() {
        try {
        deleteAllSites();

        List<Site> sites = getAllSitesFromConfig();

        List<Callable<Boolean>> indexingTasks = getIndexingTasks(sites);

        runSiteStatusTimeRefresher(sites);

        forkJoinPool.invokeAll(indexingTasks);
        }
        finally {
            setIndexingStarted(false);
            setIndexingStopped(false);
            indexingSiteIds.clear();
        }
    }

    private void deleteAllSites() {
        lemmaIndexer.deleteAll();
        pageService.deleteAllInBatch();
        siteService.deleteAllInBatch();
    }

    private List<Site> getAllSitesFromConfig() {
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

    private void runSiteStatusTimeRefresher(List<Site> sites) {
        Thread thread = new Thread(() -> {
            try {
                sites.forEach(site ->indexingSiteIds.add(site.getId()));

                while (!indexingSiteIds.isEmpty()) {
                    siteService.updateAllStatusTime(indexingSiteIds, LocalDateTime.now());
                    Thread.sleep(10_000);
                }
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private List<Callable<Boolean>> getIndexingTasks(List<Site> sites) {
        List<Callable<Boolean>> indexingTasks = new ArrayList<>();

        sites.forEach(site ->
        {
            PageInfo info = indexingPage(site, site.getUrl(), "/");
            Page mainPage = info != null ? info.getPage() : null;

            if (!determineSiteLastError(site, mainPage)) {
                Indexer indexer = new Indexer(site);
                indexer.addUniqueUrl(getValidUrlFormat(site.getUrl()));

                List<ForkJoinParser> parserTasks = new ArrayList<>(info.getChildLinks().stream()
                        .filter(link -> indexer.isCorrectUrl(link, site.getUrl()))
                        .map(link -> new ForkJoinParser(indexer, link)).toList());

                indexingTasks.add(getSingleSiteIndexingTask(indexer, parserTasks));

            }
        });
        return indexingTasks;
    }

    private PageInfo indexingPage(Site site, String pageUrl, String urlForSaving) {
        return defaultIndexer.indexing(site, pageUrl, urlForSaving);
    }

    private Callable<Boolean> getSingleSiteIndexingTask(Indexer indexer, List<ForkJoinParser> tasks) {
        return () -> {
            ForkJoinParser.invokeAll(tasks);
            Integer siteId = indexer.getSite().getId();
            indexingSiteIds.remove(siteId);

            if (indexer.getStatus() != Status.FAILED) indexer.setStatus(Status.INDEXED);
            indexer.getSite().setStatusTime(LocalDateTime.now());
            siteService.save(indexer.getSite());
            indexer.clear();
            return true;
        };
    }

    private boolean determineSiteLastError(Site site, Page mainPage) {
        String siteError = getErrorMessage(mainPage);
        site.setLastError(siteError);

        if (siteError.equals("")) return false;

        site.setStatus(Status.FAILED);
        siteService.save(site);

        return true;
    }

    private String getErrorMessage(Page page) {
        String defaultError = "???????????? ????????????????????: ???? ?????????????? ???????????????????????????????? ???????? " +
                "?????????????????? ???????????????????????? ?????????????????? ????????????.";
        if (page != null) {
            int statusCode = page.getCode();

            if (statusCode == 200) {
                return "";
            }

            if (statusCode >= 300 && statusCode < 400) {
                return "???????????? ????????????????????: url ???????????????????????????? ?????????????? ???????????????? ?????? " +
                        "????????????????(??????????????????) ??????????????, ?????????????????? ???????????????????????? ?????????????????? ????????????.";
            }
            else {
                return switch (statusCode) {
                    case 400, 403 -> "???????????? ????????????????????: ?????????? ?? ?????????????? ???? ?????????????? ??????????.";
                    case 404 -> "???????????? ????????????????????: ?????????????? ???????????????? ?????????? ???? ??????????????.";
                    case 500 -> "???????????? ????????????????????: ???????????? ?????????????? ???? ?????????????? ??????????.";
                    default -> defaultError;
                };
            }
        }
        return defaultError;
    }

    private String getValidUrlFormat(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    synchronized public boolean stopIndexing() {
        if (!isIndexingStarted) throw new IndexingException("???????????????????? ???? ????????????????",
                "attempt to stop indexing failed, reason: indexing not started", HttpStatus.FORBIDDEN);

        setIndexingStopped(true);
        log.info("The user has stopped indexing.");
        return true;
    }

    private String getValidPageUrlForSaving(String url, int rootUrlLength) {
        return url.endsWith("/") ?
                url.substring(rootUrlLength, url.length() - 1) : url.substring(rootUrlLength);
    }

    public boolean indexingUserInputPage(String pageUrl) {
        SiteConfig siteFromConfig = getSiteForReindexingPage(pageUrl);

        Site site = getSiteFromDB(siteFromConfig.getUrl());
        String validPageUrl = getValidPageUrlForSaving(pageUrl, site.getUrl().length());

        Optional<Page> optionalPage = pageService.findByPathAndSite(validPageUrl, site);

        optionalPage.ifPresent(this::deletePageForReindexing);
        PageInfo info = indexingPage(site, pageUrl, validPageUrl);

        if (info == null) {
            String error = getErrorMessage(null);
            throw new IndexingException(error, String.format("request to index page(%s) by user failed", pageUrl),
                    HttpStatus.BAD_REQUEST);
        }
        return true;
    }

    private SiteConfig getSiteForReindexingPage(String pageUrl) {
        return indexingConfig.getSites()
                .stream()
                .filter(site -> pageUrl.contains(site.getUrl())).findAny().orElseThrow(() ->
                        new IndexingException("???????????? ???????????????? ?????????????????? ???? ?????????????????? ????????????, " +
                                "?????????????????? ?? ???????????????????????????????? ??????????",
                                String.format("request to index a page(%s) outside of sites config file", pageUrl),
                                HttpStatus.BAD_REQUEST)
                );
    }

    private void deletePageForReindexing(Page page) {
        pageService.pageLemmasFrequencyDecrement(page);
        pageService.deleteById(page.getId());
    }

    private Site getSiteFromDB(String siteUrl) {
        return siteService
                .findByUrlAndStatus(getValidUrlFormat(siteUrl), Status.INDEXED)
                .orElseThrow(() -> new IndexingException("????????????: ???????? ?? ?????????????????? ?????????????????? ???? ??????????????????????????????.",
                        "request to index a page of a non-indexed site.", HttpStatus.FORBIDDEN));
    }

    @NoArgsConstructor
    public class Indexer {
        private Set<String> uniqueUrls = ConcurrentHashMap.newKeySet();
        private int siteUrlLength;
        @Getter
        private Site site;

        Indexer(Site site) {
            this.site = site;
            siteUrlLength = site.getUrl().length();
        }

        public String getUrlForSaving(String url) {
            return getValidPageUrlForSaving(url, siteUrlLength);
        }

        public PageInfo indexing(Site site, String pageUrl, String urlForSaving) {
            try {
                Connection.Response response = Jsoup.connect(pageUrl)
                        .userAgent(indexingConfig.getUserAgent())
                        .referrer(indexingConfig.getReferrer())
                        .ignoreContentType(true)
                        .execute();;

                if (!response.contentType().contains("text/html")) return null;

                return getPageInfo(response, site, urlForSaving);
            }
            catch (IOException e) {
                log.debug("Error when trying to connect to URI during indexing.", e);
            }
            return null;
        }

        private PageInfo getPageInfo(Connection.Response response, Site site, String urlForSaving)
                throws IOException {

            int statusCode = response.statusCode();

            Page page = new Page();
            page.setPath(urlForSaving);
            page.setCode(statusCode);
            page.setSite(site);

            if (statusCode == 200) {
                Document doc = response.parse();
                page.setContent(doc.outerHtml());
                pageService.save(page);

                startLemmaIndexing(site, page, doc);

                return getPageInfoFromHtml(doc, page);
            }
            else {
                page.setContent("");
                pageService.save(page);
            }
            return null;
        }

        private void startLemmaIndexing(Site site, Page page, Document html) {
            Map<Lemma, Float> lemmas = lemmaIndexer.getLemmas(site, page, html);
            synchronized (this) {
                lemmaIndexer.saveNewLemmas(lemmas);
            }
            synchronized (this) {
                lemmaIndexer.saveIndexesByLemma(lemmas, page);
            }
        }

        private PageInfo getPageInfoFromHtml(Document html, Page page) {
            if (html != null) {
                List<String> childLinks = html
                        .select("a")
                        .eachAttr("abs:href");

                return new PageInfo(page, childLinks);
            }
            return null;
        }

        public boolean isCorrectUrl(String pageUrl, String rootUrl) {
            pageUrl = getValidUrlFormat(pageUrl);
            return pageUrl.startsWith(rootUrl)
                    && !pageUrl.contains("#")
                    && !pageUrl.contains("?")
                    && !pageUrl.matches(".+(?i)(?:.jpg/?|.png/?|.pdf/?)$")
                    && addUniqueUrl(pageUrl);
        }

        public boolean checkIsIndexingStopped() {
            if (!isIndexingStopped) return false;

            site.setStatus(Status.FAILED);
            site.setLastError("???????????????????? ?????????????????????? ??????????????????????????.");
            return true;
        }

        public Status getStatus() {
            return site.getStatus();
        }

        public void setStatus(Status status) {
            site.setStatus(status);
        }

        public void clear() {
            uniqueUrls.clear();
        }

        public boolean addUniqueUrl(String pageUrl) {
            return uniqueUrls.add(pageUrl);
        }
    }
}