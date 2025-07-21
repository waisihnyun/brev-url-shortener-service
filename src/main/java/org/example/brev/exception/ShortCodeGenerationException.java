package org.example.brev.exception;

/**
 * Exception thrown when unable to generate a unique short code after maximum retry attempts
 */
public class ShortCodeGenerationException extends UrlServiceException {

    private final int maxAttempts;

    public ShortCodeGenerationException(int maxAttempts) {
        super("Unable to generate unique short code after " + maxAttempts + " attempts");
        this.maxAttempts = maxAttempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }
}
