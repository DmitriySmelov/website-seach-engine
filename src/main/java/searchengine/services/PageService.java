package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.dto.statistics.SearchPageInfo;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositorys.PageRepository;

import java.util.List;
import java.util.Optional;

@Service
public class PageService
{
    private PageRepository repository;

    public Optional<Page> findA(Integer id)
    {
        return repository.findById(id);
    }

    public PageService(PageRepository repository)
    {
        this.repository = repository;
    }

    public Page save(Page page)
    {
        return repository.save(page);
    }

    public void deleteById(int id)
    {
        repository.deleteById(id);
    }

    public List<SearchPageInfo> getSearchPageInfoByLemmas(List<String> lemmas, int limit, int offset, Integer siteId)
    {
        return repository.getSearchPageInfoByLemmas(lemmas, limit, offset, siteId);
    }

    public Iterable<Page> findAll(List<Integer> id)
    {
        return repository.findAllById(id);
    }

    public void pageLemmasFrequencyDecrement(Page page)
    {
        repository.pageLemmasFrequencyDecrement(page);
    }

    public Optional<Page> findByPathAndSite(String pageUrl, Site site)
    {
        return repository.findByPathAndSite(pageUrl,site);
    }

    public void deleteAllInBatch()
    {
        repository.deleteAllInBatch();
    }

    public int countBySite(Site site)
    {
        return repository.countBySite(site);
    }
}
