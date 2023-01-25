package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer>
{
    Optional<Lemma> findByLemmaAndSite(String lemma, Site site);

    int countBySite(Site site);

    @Query(value = "select lemma from lemmas, (select id from sites where status = 'INDEXED') as valid_sites" +
            " where lemma in (?1) and site_id = valid_sites.id group by lemma " +
            "having sum(frequency) < ((select count(*) from pages where site_id = valid_sites.id) " +
            "* ?2) order by sum(frequency) desc",
            nativeQuery = true)
    List<String> findLemmaWithValidFrequencyByAllSites(List<String> lemmas, double frequencyFactor);

    @Query(value = "select lemma from lemmas where lemma in (?1) and " +
            "site_id = ?2 group by lemma having sum(frequency) < ((select count(*) from pages " +
            "where site_id = ?2) * ?3) order by sum(frequency)",
            nativeQuery = true)
    List<String> findLemmaWithValidFrequencyBySite(List<String> lemmas, int siteId, double frequencyFactor);

    @Modifying
    @Transactional
    @Query(value = "update lemmas set frequency = lemmas.frequency + 1 where id in ?1", nativeQuery = true)
    void lemmasFrequencyIncrement(List<Integer> lemmasId);
}
