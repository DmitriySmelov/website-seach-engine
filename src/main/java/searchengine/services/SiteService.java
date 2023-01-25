package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repositorys.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class SiteService
{
    SiteRepository repository;

    @Autowired
    SiteService(SiteRepository repository)
    {
        this.repository = repository;
    }

    public Site save(Site site)
    {
        return repository.save(site);
    }

    public Integer findIndexedSitId()
    {
        return repository.findIndexedSitId();
    }

    public Integer getSiteIdIfIndexed(String siteUrl)
    {
        return repository.getSiteIdIfIndexed(siteUrl);
    }

    public void deleteAllInBatch()
    {
        repository.deleteAllInBatch();
    }

    public Optional<Site> findByUrlAndStatus(String url, Status status)
    {
        return repository.findByUrlAndStatus(url, status);
    }

    public void updateAllStatusTime(Iterable<Integer> indexingSiteIds, LocalDateTime localDateTime)
    {
        repository.updateAllStatusTime(indexingSiteIds, localDateTime);
    }

    public long count()
    {
        return repository.count();
    }

    public List<Site> findAll()
    {
        return repository.findAll();
    }
}
