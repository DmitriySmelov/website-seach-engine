package searchengine.services;

import searchengine.dto.statistics.PageSearchResponse;

public interface SearchService {

    PageSearchResponse search(String query, String siteUrl, int limit, int offset);

    String[] getRussianWordsFromText(String text);
}
