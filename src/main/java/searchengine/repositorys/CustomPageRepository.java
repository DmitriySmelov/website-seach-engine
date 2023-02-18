package searchengine.repositorys;

import searchengine.dto.search.SearchPageInfo;

import java.util.List;

public interface CustomPageRepository {

    List<SearchPageInfo> getSearchPageInfoByLemmas(List<String> lemmas, int limit, int offset, Integer siteId);
}
