package searchengine.dto.statistics;

import lombok.Data;
import lombok.Setter;

@Data
@Setter
public class TotalStatistics {

    private long sites;
    private int pages;
    private int lemmas;
    private boolean isIndexing;
}
