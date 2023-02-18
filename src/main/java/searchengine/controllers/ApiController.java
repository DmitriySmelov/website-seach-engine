package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.search.PageSearchResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.search.SearchService;
import searchengine.services.siteindexing.SiteIndexerService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final SiteIndexerService siteIndexerService;
    private SearchService searchService;

    @Autowired
    public ApiController(StatisticsService statisticsService, SearchService searchService,
                         SiteIndexerService siteIndexerService) {
        this.searchService = searchService;
        this.siteIndexerService = siteIndexerService;
        this.statisticsService = statisticsService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponse response = new IndexingResponse(siteIndexerService.stopIndexing());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        siteIndexerService.isStartIndexingPossibility();
        siteIndexerService.startIndexing();
        return ResponseEntity.ok(new IndexingResponse(true));
    }

    @GetMapping("/search")
    public ResponseEntity<PageSearchResponse> search(@RequestParam String query,
                                                     @RequestParam(required = false) String site,
                                                     @RequestParam int limit, @RequestParam int offset) {
        PageSearchResponse response = searchService.search(query, site, limit, offset);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexPage(@RequestParam String url) {
        IndexingResponse response = new IndexingResponse(siteIndexerService.indexingUserInputPage(url));
        return ResponseEntity.ok(response);
    }
}
