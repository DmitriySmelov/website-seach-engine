package searchengine.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class IndexingException extends RuntimeException {

    private HttpStatus status;
    private String userErrorMessage;

    public IndexingException(String userErrorMessage, String exceptionMessage, HttpStatus status) {
        super(exceptionMessage);
        this.userErrorMessage = userErrorMessage;
        this.status = status;
    }
}
