package searchengine.services;

import org.jsoup.nodes.Document;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Map;

public interface LemmaIndexerService
{
    void saveNewLemmas(Map<Lemma, Float> lemmas);

    void saveIndexesByLemma(Map<Lemma, Float> lemmas, Page page);

    void deleteAll();

    Map<Lemma, Float> getLemmas(Site site, Page page, Document html);
}
