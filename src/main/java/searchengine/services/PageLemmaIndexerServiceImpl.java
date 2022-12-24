package searchengine.services;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.*;

import java.util.*;

@Service
@Getter
public class PageLemmaIndexerServiceImpl implements PageLemmaIndexerService
{
    private LuceneMorphology luceneMorphology;
    private static final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ"};
    private LemmaService lemmaService;
    private IndexService indexService;
    private static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger(PageLemmaIndexerServiceImpl.class.getName());

    @Autowired
    private PageLemmaIndexerServiceImpl(LuceneMorphology luceneMorphology, LemmaService lemmaService,
                                        IndexService indexService)
    {
        this.luceneMorphology = luceneMorphology;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
    }


    public void indexingPage(Site site, Page page, Document pageHtml)
    {
        Map<String, Float> pageLemmas = indexingPageHtml(pageHtml);

        indexService.saveAll(pageLemmas.entrySet().stream().map(pageLem->
                {
                    Lemma lemma = new Lemma();
                    lemma.setLemma(pageLem.getKey());
                    lemma.setFrequency(0);
                    lemma.setSite(site);
                    lemma.setId(lemmaService.save(lemma));


                    Index index = new Index();
                    index.setPage(page);
                    index.setLemma(lemma);
                    index.setGrade(pageLem.getValue());
                    return index;
                }
        ).toList());
    }

    private Map<String, Float> indexingPageHtml(Document html)
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
        String[] words = getRussianWordsFromText(text);

        for (String word : words)
        {
            if (word.isBlank()) continue;

            List<String> wordMorphInfo = luceneMorphology.getMorphInfo(word);
            if (checkIsWordOneOfParticle(wordMorphInfo) || word.length() < 2)
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

    private String[] getRussianWordsFromText(String text)
    {
        return text
                .toLowerCase()
                .replaceAll("ё", "е")
                .split("[^а-я]+");
    }
}
