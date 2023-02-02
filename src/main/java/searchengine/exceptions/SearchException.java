package searchengine.exceptions;

import org.springframework.http.HttpStatus;

public class SearchException extends IndexingException {

    public SearchException(String userErrorMessage, String exceptionMessage, HttpStatus status) {
        super(userErrorMessage, exceptionMessage, status);
    }
}
