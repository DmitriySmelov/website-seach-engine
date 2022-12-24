package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer>
{
    @Modifying
    @Transactional
    void deleteByUrl(String siteUrl);

    Optional<Site> findByUrl(String siteUrl);

    @Modifying
    @Transactional
    @Query(value = "update lemmas set frequency = lemmas.frequency + " +
            "(select count(id) from `indexes` where lemmas.id = `indexes`.lemma_id and " +
            "`indexes`.page_id in (select id from pages where site_id = lemmas.site_id)) " +
            "where site_id = :#{#site.id}",
            nativeQuery = true)
    void siteLemmasFrequencyIncrement(Site site);
}
