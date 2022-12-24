package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;

@Service
public class ConcurrentSiteIndexerService
{
    PageService pageService;
    LemmaService lemmaService;

    @Autowired
    public ConcurrentSiteIndexerService(PageService pageService, LemmaService lemmaService)
    {
        this.pageService = pageService;
        this.lemmaService = lemmaService;
    }

    synchronized public int save(Lemma lemma)
    {
        return lemmaService.save(lemma);
    }

    synchronized public boolean checkIsPageNew(Page page)
    {
        return pageService.checkIsPageNew(page);
    }

    synchronized public void threadSleep(int millis) throws InterruptedException
    {
        Thread.sleep(millis);
    }
}
