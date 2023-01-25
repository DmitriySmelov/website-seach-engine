package searchengine.exceptions;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import searchengine.dto.statistics.IndexingErrorResponse;

@ControllerAdvice
public class ControllerAdvisor extends ResponseEntityExceptionHandler
{
    private final Logger log = LogManager.getLogger(ControllerAdvisor.class.getName());

    @ExceptionHandler({IndexingException.class, SearchException.class})
    public ResponseEntity<IndexingErrorResponse> handleIndexingExceptions(
            IndexingException ex, WebRequest request)
    {
        log.info(ex);

        IndexingErrorResponse response = new IndexingErrorResponse(ex.getUserErrorMessage());
        return new ResponseEntity<>(response, ex.getStatus());
    }
}
