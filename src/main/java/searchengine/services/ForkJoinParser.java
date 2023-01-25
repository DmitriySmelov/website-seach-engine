package searchengine.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import searchengine.dto.statistics.PageInfo;
import searchengine.model.Site;
import searchengine.services.SiteIndexerServiceImpl.Indexer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveAction;

public class ForkJoinParser extends RecursiveAction
{
    private Indexer indexer;
    private String pageUrl;
    private Site site;
    private final Logger log = LogManager.getLogger(ForkJoinParser.class.getName());

    public ForkJoinParser(Indexer indexer, String pageUrl)
    {
        this.indexer = indexer;
        this.pageUrl = pageUrl;
        site = indexer.getSite();
    }

    @Override
    protected void compute()
    {
        if(!indexer.checkIsIndexingStopped())
        {
            PageInfo info = indexPage(site, pageUrl);
            pageUrl = null;
            if(info != null)
            {
                List<String> childPages = info.getChildLinks();

                ArrayList<ForkJoinParser> taskList = new ArrayList<>();

                childPages.stream()
                        .filter(link -> indexer.isCorrectUrl(link, site.getUrl()))
                        .forEach(link ->
                        {
                            ForkJoinParser task = new ForkJoinParser(indexer, link);
                            taskList.add(task);
                        });
                info = null;
                pageUrl = null;

                if(!indexer.checkIsIndexingStopped()) ForkJoinParser.invokeAll(taskList);
            }
        }
    }

    private PageInfo indexPage(Site site, String pageUrl)
    {
        try
        {
            Thread.sleep(500);
            String urlForSaving = indexer.getUrlForSaving(pageUrl);
            PageInfo info = indexer.indexing(site, pageUrl, urlForSaving);

            urlForSaving = null;

            return info;
        }
        catch (InterruptedException e)
        {
            log.error("Sleeping between requests aborted.", e);
        }

        return null;
    }
}