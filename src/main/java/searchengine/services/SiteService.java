package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.model.Site;
import searchengine.repositorys.SiteRepository;

import java.util.Optional;

@Service
public class SiteService {
    SiteRepository repository;

    SiteService(SiteRepository repository)
    {
        this.repository = repository;
    }

    public void siteLemmasFrequencyIncrement(Site site)
    {
        repository.siteLemmasFrequencyIncrement(site);

    }

    public void deleteByUrl(String siteUrl)
    {
        repository.deleteByUrl(siteUrl);
    }

    public Optional<Site> findByUrl(String siteUrl)
    {
        return repository.findByUrl(siteUrl);
    }

    public Site save(Site site)
    {
        return repository.save(site);
    }
}
