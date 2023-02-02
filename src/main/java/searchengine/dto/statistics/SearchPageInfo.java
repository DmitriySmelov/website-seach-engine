package searchengine.dto.statistics;

import lombok.*;

import java.math.BigInteger;

@Data
@NoArgsConstructor
@Getter
@Setter
public class SearchPageInfo {

    private Integer id;
    private Double relevance;
    private BigInteger count;
}

