package searchengine.services;

import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LemmaIndexerServiceImpl implements LemmaIndexerService
{
    private final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
    private LuceneMorphology luceneMorphology;
    private LemmaService lemmaService;
    private IndexService indexService;
    private SearchService searchService;

    public LemmaIndexerServiceImpl(LuceneMorphology luceneMorphology, LemmaService lemmaService,
                                   IndexService indexService, SearchService searchService)
    {
        this.luceneMorphology = luceneMorphology;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.searchService = searchService;
    }

    public Map<Lemma, Float> getLemmas(Site site, Page page, Document html)
    {
        Map<String, Float> pageLemmas = getLemmasFromPageHtml(html);

        Map<Lemma, Float> lemmas = new HashMap<>();

        pageLemmas.forEach((lem, grade) -> {
            Lemma lemma = new Lemma();
            lemma.setLemma(lem);
            lemma.setFrequency(0);
            lemma.setSite(site);

            lemmas.put(lemma, grade);
        });
        pageLemmas.clear();

        return lemmas;
    }

    public void saveNewLemmas(Map<Lemma, Float> lemmas)
    {
        List<Lemma> lemmasForSaving =  lemmas.keySet().stream()
                .filter(lem -> !lemmaService.findIfExist(lem)).collect(Collectors.toList());

        if(lemmasForSaving.isEmpty()) return;

        lemmaService.saveAll(lemmasForSaving);

        lemmasForSaving.clear();
    }

    public void saveIndexesByLemma(Map<Lemma, Float> lemmas, Page page)
    {
        List<Index> indexes = getIndexes(lemmas, page);
        indexService.saveAll(indexes);

        List<Integer> lemmasId = lemmas.keySet().stream().map(Lemma::getId).collect(Collectors.toList());

        lemmaService.lemmasFrequencyIncrement(lemmasId);

        lemmasId.clear();
        lemmas.clear();
        indexes.clear();
    }


    private List<Index> getIndexes(Map<Lemma, Float> lemmas, Page page)
    {
        return lemmas.entrySet().stream().map(lem->
                {
                    Index index = new Index();
                    index.setPage(page);
                    index.setLemma(lem.getKey());
                    index.setGrade(lem.getValue());
                    return index;
                }
        ).collect(Collectors.toList());
    }

    public void deleteAll()
    {
        indexService.deleteAllInBatch();
        lemmaService.deleteAllInBatch();
    }

    private Map<String, Float> getLemmasFromPageHtml(Document html)
    {
        Map<String, Float> pageLemmas = new HashMap<>();
        collectLemmas(html.title(), 1F, pageLemmas);

        html.body().getAllElements().forEach(element -> {
            String text = element.ownText();
            if(!text.isBlank()) collectLemmas(text, 0.8F, pageLemmas);
        });
        return pageLemmas;
    }

    private void collectLemmas(String text, float gradeFactor, Map<String, Float> mapForSavingLemmas)
    {
        String[] words = searchService.getRussianWordsFromText(text);

        for (String word : words)
        {
            if (word.isBlank()) continue;

            List<String> wordMorphInfo = luceneMorphology.getMorphInfo(word);
            if (checkIsWordOneOfParticle(wordMorphInfo))
            {
                continue;
            }
            List<String> allFormsOfWord = luceneMorphology.getNormalForms(word);
            String normalFormOfWord = allFormsOfWord.get(0);

            if (mapForSavingLemmas.containsKey(normalFormOfWord)) {
                mapForSavingLemmas.put(normalFormOfWord, mapForSavingLemmas.get(normalFormOfWord) + gradeFactor);
            } else {
                mapForSavingLemmas.put(normalFormOfWord, gradeFactor);
            }
        }
    }

    private boolean checkIsWordOneOfParticle(List<String> wordMorphInfo)
    {
        return wordMorphInfo.stream().anyMatch(this::wordHasParticleProperty);
    }

    private boolean wordHasParticleProperty(String wordBase)
    {
        return Arrays.stream(particlesNames).anyMatch(wordBase::contains);
    }
}
