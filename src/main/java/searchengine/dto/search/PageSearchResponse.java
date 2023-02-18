package searchengine.dto.search;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.List;

@Data
@Setter
@Getter
public class PageSearchResponse {

    private boolean result;
    private BigInteger count;
    private List<PageSearchData> data;
}
