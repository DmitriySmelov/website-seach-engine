package searchengine.services;

import lombok.Setter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.IndexingConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.statistics.PageInfo;
import searchengine.exceptions.IndexingException;
import searchengine.model.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

@Service
public class SiteIndexerServiceImpl implements SiteIndexerService
{
    @Setter
    private volatile boolean isIndexingStarted;
    @Setter
    private volatile boolean isIndexingStopped;
    private final PageLemmaIndexerServiceImpl pageLemmaIndexer;
    private final SiteService siteService;
    private final IndexingConfig indexingConfig;
    private final PageService pageService;
    private final Logger log = LogManager.getLogger(SiteIndexerServiceImpl.class.getName());

    @Autowired
    public SiteIndexerServiceImpl(SiteService siteService, IndexingConfig indexingConfig,
                                  PageLemmaIndexerServiceImpl pageLemmaIndexer, PageService pageService)
    {
        this.siteService = siteService;
        this.indexingConfig = indexingConfig;
        this.pageLemmaIndexer = pageLemmaIndexer;
        this.pageService = pageService;
    }

    public boolean checkIsIndexingPossibility()
    {
        if (isIndexingStarted) throw new IndexingException("Индексация уже запущена.",
                "attempt to start indexing failed, reason: indexing is already started", HttpStatus.FORBIDDEN);

        setIndexingStarted(true);
        return true;
    }

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
        }
    }

    private void indexingAllSites()
    {
        ArrayList<ForkJoinParser> tasks = new ArrayList<>();
        List<Site> sites = getAllSitesFromConfig();

        sites.forEach(site ->
        {
            String siteUrl = site.getUrl();
            tasks.add(new ForkJoinParser(siteUrl, site));
        });

        ForkJoinParser.invokeAll(tasks);

        sites.forEach(site ->
        {
            if(!checkSiteForError(site)) site.setStatus(Status.INDEXED);

            pageService.formatAllPagesUrlBySite(site);
            siteService.siteLemmasFrequencyIncrement(site);
        });
    }

    private List<Site> getAllSitesFromConfig()
    {
        return indexingConfig.getSites().stream().map(s -> {
            String siteUrl = getValidSiteUrlFormat(s.getUrl());

            Site site = new Site();
            siteService.deleteByUrl(siteUrl);
            site.setStatus(Status.INDEXING);
            site.setStatusTime(new Date());
            site.setName(s.getName());
            site.setUrl(siteUrl);
            siteService.save(site);
            return site;
        }).toList();
    }

    private String getValidSiteUrlFormat(String siteUrl)
    {
        siteUrl.trim();
        return siteUrl.endsWith("/") ? siteUrl : siteUrl.concat("/");
    }

    private String getValidPageUrlFormat(String siteUrl, String pageUrl)
    {
        return pageUrl.replaceFirst(siteUrl, "/");
    }

    private boolean checkSiteForError(Site site)
    {
        if(site.getStatus() == Status.FAILED) return true;

        String siteError = getErrorMessage(site.getUrl());

        if(siteError == null) return false;

        site.setStatus(Status.FAILED);
        site.setLastError(siteError);
        return true;
    }

    private String getErrorMessage(String pageUrl)
    {
        String defaultError = "Ошибка индексации: не удалось проиндексировать сайт " +
                "проверьте корректность введенных данных.";
        Optional<Page> opPage = pageService.findByPath(pageUrl);
        if (opPage.isPresent())
        {
            int statusCode = opPage.get().getCode();

            if (statusCode == 200)
            {
                return null;
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


    public boolean stopIndexing()
    {
        if (!isIndexingStarted) throw new IndexingException("Индексация не запущена",
                "attempt to stop indexing failed, reason: indexing not started", HttpStatus.FORBIDDEN);

        setIndexingStopped(true);
        log.info("The user has stopped indexing.");
        return true;
    }

    public boolean indexingUserInputPage(String pageUrl)    //не проверял
    {
        SiteConfig siteFromConfig = getSiteFromConfigByPageUrl(pageUrl);

        Site site = getSiteFromDB(siteFromConfig.getUrl());
        String validPageUrl = getValidPageUrlFormat(site.getUrl(), pageUrl);

        Optional<Page> pageFromDb = pageService.findByPath(validPageUrl);
        if(pageFromDb.isPresent())
        {
            Page page = pageFromDb.get();
            pageService.pageLemmasFrequencyDecrement(page);
            pageService.deleteById(page.getId());
        }
        try
        {
            PageInfo info = indexingPage(site, pageUrl);
            if (info != null )
            {
                Page page = info.getPage();
                pageService.pageLemmasFrequencyIncrement(page);
            }
        }
        catch (IOException e)
        {
            //TODO
            e.printStackTrace();
        }
        catch (Exception ex)
        {
            //TODO
            ex.printStackTrace();
        }
        String error = getErrorMessage(validPageUrl);
        if(error == null) return true;

        throw new IndexingException(error, String.format("request to index page(%s) by user failed",pageUrl),
                HttpStatus.BAD_REQUEST);
    }

    private SiteConfig getSiteFromConfigByPageUrl(String pageUrl)
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

    private Site getSiteFromDB(String siteUrl)
    {
        return siteService
                .findByUrl(getValidSiteUrlFormat(siteUrl))
                .orElseThrow(() -> new IndexingException("Ошибка: сайт с введенной страницей не проиндексирован.",
                        "request to index a page of a non-indexed site.", HttpStatus.FORBIDDEN));
    }

    private PageInfo indexingPage(Site site, String pageUrl)
            throws IOException {
        Connection.Response response = Jsoup.connect(pageUrl)
                .userAgent(indexingConfig.getUserAgent())
                .referrer(indexingConfig.getReferrer())
                .execute();

        int statusCode = response.statusCode();
        Document doc = response.parse();

        String html = doc.outerHtml();

        Page page = new Page();
        page.setPath(pageUrl);
        page.setCode(statusCode);
        page.setSite(site);
        page.setContent(html);
        if(!pageService.checkIsPageNew(page)) return null;

        pageLemmaIndexer.indexingPage(site, page, doc);

        if( page.getCode() == 200 )
        {

            List<String> childLinks = doc
                    .select("a:not(:has(img)):not([type])")
                    .eachAttr("abs:href");
            PageInfo info = new PageInfo(page, childLinks);
            return info;
        }
        return null;
    }

    private class ForkJoinParser extends RecursiveAction
    {
        private String pageUrl;
        private Site site;

        private ForkJoinParser(String pageUrl, Site site)
        {
            this.pageUrl = pageUrl;
            this.site= site;
        }

        @Override
        protected void compute()
        {
            if(!checkIsIndexingStopped())
            {
                PageInfo info = indexPage(site, pageUrl);
                if(info != null)
                {
                    List<String> childPages = info.getChildLinks();


                    invokeTasksForChildPages(childPages);
                }
            }
        }

        private PageInfo indexPage(Site site, String pageUrl)
        {

                if(pageService.existsByPath(pageUrl)) return null;

                try
                {
                    Thread.sleep(500);
                    return indexingPage(site, pageUrl);
                }
                catch (InterruptedException e)
                {
                    log.error("Sleeping between requests aborted.", e);
                }
                catch (IOException e)
                {
                    log.debug("Error when trying to connect to URI during indexing.", e);
                }

            return null;
        }

        private void invokeTasksForChildPages(List<String> childPages)
        {
            ArrayList<ForkJoinParser> taskList = new ArrayList<>();

            childPages.stream()
                    .filter(link -> !link.contains("#") && link.startsWith(site.getUrl()))
                    .forEach(link ->
                    {
                        ForkJoinParser task = new ForkJoinParser(link, site);
                        taskList.add(task);
                    });

            if(!checkIsIndexingStopped()) ForkJoinParser.invokeAll(taskList);
        }

        private boolean checkIsIndexingStopped()
        {
            if(!isIndexingStopped) return false;

            site.setStatus(Status.FAILED);
            site.setLastError("Индексация остановлена пользователем.");
            return true;
        }
    }
}