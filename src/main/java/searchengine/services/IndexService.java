package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.Index;
import searchengine.repositorys.IndexRepository;

import java.util.List;

@Service
public class IndexService
{
    IndexRepository repository;

    @Autowired
    IndexService(IndexRepository repository)
    {
        this.repository = repository;
    }

    public void saveAll(List<Index> indexes)
    {
        repository.saveAll(indexes);
    }

    public void deleteAllInBatch()
    {
        repository.deleteAllInBatch();
    }

}
