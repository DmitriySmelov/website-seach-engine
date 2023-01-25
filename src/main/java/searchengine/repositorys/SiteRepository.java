package searchengine.repositorys;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Site;
import searchengine.model.Status;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface SiteRepository extends JpaRepository<Site, Integer>
{
    @Query(value = "select id from sites where status = 'INDEXED' limit 1", nativeQuery = true)
    Integer findIndexedSitId();

    @Query(value = "select id from sites where status = 'INDEXED' and url = ?1", nativeQuery = true)
    Integer getSiteIdIfIndexed(String siteUrl);

    Optional<Site> findByUrlAndStatus(String url, Status status);

    @Modifying
    @Transactional
    @Query(value = "update sites set status_time = ?2 where id in ?1", nativeQuery = true)
    void updateAllStatusTime(Iterable<Integer> indexingSiteIds, LocalDateTime localDateTime);
}
