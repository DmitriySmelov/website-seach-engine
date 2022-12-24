package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.model.Page;
import searchengine.repositorys.IndexRepository;

@Service
public class IndexService
{
    IndexRepository repository;

    @Autowired
    IndexService(IndexRepository repository)
    {
        this.repository = repository;
    }

    public Iterable<Index> saveAll(Iterable<Index> indexes)
    {
        return repository.saveAll(indexes);
    }

}
