package searchengine.dto.indexing;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class IndexingErrorResponse {

    private boolean result = false;
    private final String error;
}
