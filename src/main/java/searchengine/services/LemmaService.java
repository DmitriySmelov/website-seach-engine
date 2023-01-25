package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Site;
import searchengine.repositorys.LemmaRepository;

import java.util.List;
import java.util.Optional;

@Service
public class LemmaService
{
    LemmaRepository repository;

    @Autowired
    LemmaService(LemmaRepository repository)
    {
        this.repository = repository;
    }

    public void saveAll(List<Lemma> lemmas)
    {
        repository.saveAll(lemmas);
    }

    public boolean findIfExist(Lemma lemma)
    {

        Optional<Lemma> lemmaFromDB = repository.findByLemmaAndSite(lemma.getLemma(), lemma.getSite());

        if(lemmaFromDB.isEmpty()) return false ;

        lemma.setId(lemmaFromDB.get().getId());
        lemma.setFrequency(lemmaFromDB.get().getFrequency() + 1);
        return true;
    }

    public List<String> chooseLemmaWithValidFrequencyByAllSites(List<String> lemmas, double frequencyFactor)
    {
        return repository.findLemmaWithValidFrequencyByAllSites(lemmas, frequencyFactor);
    }

    public List<String> chooseLemmaWithValidFrequencyBySite(List<String> lemmas, int siteId, double frequencyFactor)
    {
        return repository.findLemmaWithValidFrequencyBySite(lemmas, siteId, frequencyFactor);
    }

    public void deleteAllInBatch()
    {
        repository.deleteAllInBatch();
    }

    public void lemmasFrequencyIncrement(List<Integer> lemmasId)
    {
        repository.lemmasFrequencyIncrement(lemmasId);
    }

    public int countBySite(Site site)
    {
        return repository.countBySite(site);
    }
}

