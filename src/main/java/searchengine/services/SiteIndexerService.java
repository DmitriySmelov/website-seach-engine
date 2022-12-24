package searchengine.services;

import searchengine.model.Page;

public interface SiteIndexerService
{
    void startIndexing();

    boolean stopIndexing();

    boolean checkIsIndexingPossibility();

    boolean indexingUserInputPage(String pageUrl);
}
