package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.repositorys.LemmaRepository;

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

    synchronized public int save(Lemma lemma)
    {
        Optional<Lemma> lemmaFromDB = repository.findByLemmaAndSite(lemma.getLemma(), lemma.getSite());
        return lemmaFromDB.isEmpty() ? repository.save(lemma).getId() : lemmaFromDB.get().getId();
    }
}
