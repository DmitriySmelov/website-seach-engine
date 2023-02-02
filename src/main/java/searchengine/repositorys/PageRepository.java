package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer>, CustomPageRepository {

    int countBySite(Site site);

    Optional<Page> findByPathAndSite(String path, Site site);

    @Modifying
    @Transactional
    @Query(value = "update lemmas set frequency = lemmas.frequency - " +
            "(select count(id) from `indexes` where lemmas.id = `indexes`.lemma_id and " +
            "`indexes`.page_id = :#{#page.id}) where lemmas.site_id = :#{#page.site}",
            nativeQuery = true)
    void pageLemmasFrequencyDecrement(Page page);

}
