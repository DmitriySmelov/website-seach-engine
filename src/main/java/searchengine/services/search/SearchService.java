package searchengine.services.search;

import searchengine.dto.search.PageSearchResponse;

public interface SearchService {

    PageSearchResponse search(String query, String siteUrl, int limit, int offset);

    String[] getRussianWordsFromText(String text);
}
