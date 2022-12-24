package searchengine.dto.statistics;

import lombok.Data;

@Data
public class IndexingResponse {
    boolean result;

    public IndexingResponse(boolean result)
    {
        this.result =result;
    }
}
