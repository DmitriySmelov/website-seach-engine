package searchengine.services.search;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.dto.search.PageSearchData;
import searchengine.dto.search.PageSearchResponse;
import searchengine.dto.search.SearchPageInfo;
import searchengine.exceptions.IndexingException;
import searchengine.model.*;
import searchengine.services.LemmaService;
import searchengine.services.PageService;
import searchengine.services.SiteService;

import java.math.BigInteger;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SearchServiceImpl implements SearchService {

    private double frequencyFactor = 0.9;
    private final LuceneMorphology luceneMorphology;
    private final LemmaService lemmaService;
    private final PageService pageService;
    private final SiteService siteService;

    @Autowired
    private SearchServiceImpl(LuceneMorphology luceneMorphology, LemmaService lemmaService,
                              PageService pageService, SiteService siteService) {
        this.siteService = siteService;
        this.luceneMorphology = luceneMorphology;
        this.lemmaService = lemmaService;
        this.pageService = pageService;
    }

    public PageSearchResponse search(String query, String siteUrl, int limit, int offset) {
        Integer siteId = checkIsSearchPossibilityAndGetInfoForSearch(siteUrl);

        String[] russianLemmas = getRussianWordsFromText(query.trim());

        List<String> lemmasForSearch = Arrays.stream(russianLemmas)
                .map(lem -> luceneMorphology.getNormalForms(lem).get(0)).toList();

        List<String> lemmas = siteUrl == null ?
                lemmaService.chooseLemmaWithValidFrequencyByAllSites(lemmasForSearch, frequencyFactor) :
                lemmaService.chooseLemmaWithValidFrequencyBySite(lemmasForSearch, siteId, frequencyFactor);

        List<SearchPageInfo> pageInfo = pageService.getSearchPageInfoByLemmas(lemmas, limit, offset, siteId);

        PageSearchResponse response = new PageSearchResponse();
        response.setResult(true);

        if (pageInfo.size() == 0) {
            response.setCount(BigInteger.valueOf(0));
            response.setData(new ArrayList<>());
            return response;
        }
        response.setCount(pageInfo.get(0).getCount());

        List<PageSearchData> searchData = getSearchData(pageInfo, lemmas);

        response.setData(searchData);

        return response;
    }

    private List<PageSearchData> getSearchData(List<SearchPageInfo> pagesInfo, List<String> lemmas) {
        List<PageSearchData> searchData = new ArrayList<>();

        Map<Integer, Double> pageRelevanceMap = pagesInfo.stream()
                .collect(Collectors.toMap(SearchPageInfo::getId, SearchPageInfo::getRelevance));

        Iterable<Page> list = pageService.findAll(
                pagesInfo.stream().map(SearchPageInfo::getId).collect(Collectors.toList()));

        list.forEach(page -> searchData.add(getDataFromPageInfo(page, lemmas, pageRelevanceMap)));

        return searchData;
    }

    private Integer checkIsSearchPossibilityAndGetInfoForSearch(String siteUrl) {
        boolean isSearchByAllSites = siteUrl == null;

        Integer indexedSiteId = isSearchByAllSites ?
                siteService.findIndexedSitId() : siteService.getSiteIdIfIndexed(siteUrl);

        if (indexedSiteId == null) {
            String userError = siteUrl == null ? "сайтов" : "введенного сайта";
            String logError = siteUrl == null ? "sites" : "site " + siteUrl;
            throw new IndexingException(String.format("Не удалось совершить поиск, причина: " +
                    "индексация %s не произведена.", userError),
                    String.format("Search query is failed. Reason: %s not indexed.", logError),
                    HttpStatus.FORBIDDEN);
        }
        return isSearchByAllSites ? null : indexedSiteId;
    }

    private PageSearchData getDataFromPageInfo(Page page, List<String> lemmas, Map<Integer, Double> pageRelevanceMap) {
        Document doc = Jsoup.parse(page.getContent());
        StringBuilder text = new StringBuilder();

        doc.getAllElements().forEach(el ->
        {
            String elText = el.ownText();
            if (!elText.isBlank()) text.append(el.ownText()).append(" ");
        });

        PageSearchData data = new PageSearchData();
        data.setSite(page.getSite().getUrl());
        data.setSiteName(page.getSite().getName());
        data.setUri(page.getPath());
        data.setRelevance(pageRelevanceMap.get(page.getId()));
        data.setTitle(doc.title());

        SnippetFinder finder = new SnippetFinder(200, 30);

        data.setSnippet(finder.getSnippet(text.toString(), lemmas));

        return data;
    }

    public String[] getRussianWordsFromText(String text) {
        return text
                .toLowerCase()
                .replaceAll("ё", "е")
                .split("[^а-яе]+");
    }

    private class SnippetFinder {
        private int snippetLength;
        private int lettersBeforeFirstSnippetLemma;
        private int begin;
        private int finish;
        private boolean isSubstring;
        private String text;
        private StringBuilder snippet;
        private Pattern pattern;

        private SnippetFinder(int snippetLength, int lettersBeforeFirstSnippetLemma) {
            this.snippetLength = snippetLength;
            this.lettersBeforeFirstSnippetLemma = lettersBeforeFirstSnippetLemma;
        }

        private StringBuilder getSnippet(String text, List<String> lemmas) {
            this.text = text;
            snippet = new StringBuilder();
            pattern = Pattern.compile(getSearchRegex(lemmas));
            Matcher matcher = pattern.matcher(text.toLowerCase());

            snippet.append("...");

            snippet.append(getSnippetFromText(new HashSet<>(lemmas), matcher));

            if (snippet.length() < snippetLength) snippet.append(this.text.substring(begin));
            snippet.append("...");

            return snippet;
        }

        private StringBuilder getSnippetFromText(Set<String> lemmas, Matcher match) {
            StringBuilder snippet = new StringBuilder();
            while (match.find()) {
                String word = match.group(1).replaceAll("ё", "е");
                if (!lemmas.contains(luceneMorphology.getNormalForms(word).get(0))) continue;

                int start = match.start() + 1;
                int end = match.end() - 1;

                if (finish == 0) {
                    begin = start <= lettersBeforeFirstSnippetLemma ? 0 : start - lettersBeforeFirstSnippetLemma;
                    finish = begin + snippetLength;
                    if (match.start() == 0 && text.charAt(0) == word.charAt(0)) start = 0;

                    isSubstring = true;
                }
                if (start != 0) snippet.append(text, begin, start);
                snippet.append("<b>").append(word).append("</b>");
                if (isSubstring) {
                    begin = 0;
                    text = text.substring(end, finish);
                    match = pattern.matcher(text);
                    isSubstring = false;
                    continue;
                }
                begin = end;
            }
            return snippet;
        }

        private String getSearchRegex(List<String> lemmas) {
            StringBuilder regex = new StringBuilder();
            String russianLetters = "[а-яё]";
            String startOfRegex = "(?:[^а-яё]|^)(";
            String endOfRegex = ")(?:[^а-яё]|$)";
            int lemmasSize = lemmas.size() - 1;

            regex.append(startOfRegex);
            for (int i = 0; i <= lemmasSize; i++) {
                String lemma = lemmas.get(i);
                regex.append("(");
                switch (lemma.length()) {
                    case 1, 2 -> regex.append(getLemmaRegex(lemma));
                    case 3 -> regex.append(getLemmaRegex(lemma)).append(russianLetters).append("{0,2}");
                    case 4 -> regex.append(getLemmaRegex(lemma.substring(0, lemma.length() - 1)))
                            .append(russianLetters).append("{0,3}");
                    default -> regex.append(getLemmaRegex(lemma.substring(0, lemma.length() - 2)))
                            .append(russianLetters).append("{0,4}");
                }
                regex.append(")");
                if (i != lemmasSize) regex.append("|");
            }
            regex.append(endOfRegex);
            return regex.toString();
        }

        private String getLemmaRegex(String lemma) {
            return lemma.replaceAll("[её]", "[её]");
        }
    }
}
