package org.example.brev.exception;

/**
 * Base exception for all URL service related errors
 */
public class UrlServiceException extends RuntimeException {

    public UrlServiceException(String message) {
        super(message);
    }

    public UrlServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
