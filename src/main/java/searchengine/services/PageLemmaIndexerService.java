package searchengine.services;

import org.jsoup.nodes.Document;
import searchengine.model.Page;
import searchengine.model.Site;

public interface PageLemmaIndexerService
{
    void indexingPage(Site site, Page page, Document pageHtml);
}
