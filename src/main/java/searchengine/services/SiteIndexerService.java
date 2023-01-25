package searchengine.services;

public interface SiteIndexerService
{
    void startIndexing();

    boolean stopIndexing();

    boolean isStartIndexingPossibility();

    boolean indexingUserInputPage(String pageUrl);

    boolean isIndexingStarted();
}
