package searchengine.repositorys;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface LemmaRepository extends CrudRepository<Lemma, Integer>
{
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);
}
