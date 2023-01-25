package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.model.Site;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService
{
    private LemmaService lemmaService;
    private SiteService siteService;
    private PageService pageService;
    private SiteIndexerService siteIndexerService;

    @Autowired
    StatisticsServiceImpl(SiteService siteService, PageService pageService,
                          LemmaService lemmaService, SiteIndexerService siteIndexerService)
    {
        this.lemmaService = lemmaService;
        this.pageService = pageService;
        this.siteService = siteService;
        this.siteIndexerService = siteIndexerService;
    }

    @Override
    public StatisticsResponse getStatistics()
    {
        TotalStatistics total = new TotalStatistics();
        total.setSites(siteService.count());
        total.setIndexing(siteIndexerService.isIndexingStarted());

        List<DetailedStatisticsItem> detailed = new ArrayList<>();
        List<Site> sitesList = siteService.findAll();
        for (Site site : sitesList)
        {
            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(site.getName());
            item.setUrl(site.getUrl());
            int pages = pageService.countBySite(site);
            int lemmas = lemmaService.countBySite(site);
            item.setPages(pages);
            item.setLemmas(lemmas);
            item.setStatus(String.valueOf(site.getStatus()));
            item.setError(site.getLastError());
            item.setStatusTime(site.getStatusTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);
            detailed.add(item);
        }
        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);
        return response;
    }
}
