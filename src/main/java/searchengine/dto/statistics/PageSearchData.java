package searchengine.dto.statistics;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class PageSearchData {

    private String site;
    private String siteName;
    private String uri;
    private String title;
    private StringBuilder snippet;
    private Double relevance;
}
