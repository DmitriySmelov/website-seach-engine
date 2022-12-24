package searchengine.repositorys;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.Optional;

@Repository
public interface PageRepository extends CrudRepository<Page, Integer>
{
    boolean existsByPath(String path);

    Optional<Page> findByPath(String path);

    @Modifying
    @Transactional
    @Query(value = "UPDATE pages SET path = (REPLACE(path, :#{#site.url}, '/')) where pages.site_id = :#{#site.id}"
            , nativeQuery = true)
    void formatAllPagesUrlBySite(Site site);

    @Modifying
    @Transactional
    @Query(value = "update lemmas set frequency = lemmas.frequency - " +
            "(select count(id) from `indexes` where lemmas.id = `indexes`.lemma_id and " +
            "`indexes`.page_id = :#{#page.id}) where lemmas.site_id = :#{#page.site}",
            nativeQuery = true)
    void pageLemmasFrequencyDecrement(Page page);

    @Modifying
    @Transactional
    @Query(value = "update lemmas set frequency = lemmas.frequency + " +
            "(select count(id) from `indexes` where lemmas.id = `indexes`.lemma_id and " +
            "`indexes`.page_id = :#{#page.id}) where lemmas.site_id = :#{#page.site}",
            nativeQuery = true)
    void pageLemmasFrequencyIncrement(Page page);
}
