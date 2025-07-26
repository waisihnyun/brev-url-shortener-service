package org.example.brev.exception;

/**
 * Exception thrown when a short code is not found in the system
 */
public class ShortCodeNotFoundException extends UrlServiceException {

    private final String shortCode;

    public ShortCodeNotFoundException(String shortCode) {
        super(String.format("Short code '%s' not found", shortCode));
        this.shortCode = shortCode;
    }

    public String getShortCode() {
        return shortCode;
    }
}
