package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.dto.statistics.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Page;
import searchengine.services.SiteIndexerService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexerService siteIndexerService;

    @Autowired
    public ApiController(StatisticsService statisticsService,
                         SiteIndexerService siteIndexerService)
    {
        this.siteIndexerService = siteIndexerService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing()
    {
        IndexingResponse response = new IndexingResponse(siteIndexerService.stopIndexing());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing()
    {
        IndexingResponse response = new IndexingResponse(siteIndexerService.checkIsIndexingPossibility());
        siteIndexerService.startIndexing();
//        siteIndexerService.indexingUserInputPage("https://artmuseum26.ru/");

        return ResponseEntity.ok(new IndexingResponse(true));
    }
}
