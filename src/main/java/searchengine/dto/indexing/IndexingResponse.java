package searchengine.dto.indexing;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Setter
@Getter
public class IndexingResponse {

    private boolean result;

    public IndexingResponse(boolean result) {
        this.result = result;
    }
}
