package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositorys.PageRepository;

import java.util.Optional;

@Service
public class PageService
{
    private PageRepository repository;

    public PageService(PageRepository repository)
    {
        this.repository = repository;
    }

    synchronized public boolean checkIsPageNew(Page page)
    {
        Optional<Page> pageFromDB = repository.findByPath(page.getPath());
        if(pageFromDB.isPresent()) return false;

        repository.save(page);
        return true;
    }

    public void deleteById(int id)
    {
        repository.deleteById(id);
    }

    void formatAllPagesUrlBySite(Site site)
    {
        repository.formatAllPagesUrlBySite(site);
    }

    public Optional<Page> findByPath(String path)
    {
        return repository.findByPath(path);
    }

    public boolean existsByPath(String path)
    {
        return repository.existsByPath(path);
    }

    public void pageLemmasFrequencyDecrement(Page page)
    {
        repository.pageLemmasFrequencyDecrement(page);
    }

    public void pageLemmasFrequencyIncrement(Page page)
    {
        repository.pageLemmasFrequencyIncrement(page);
    }
}
