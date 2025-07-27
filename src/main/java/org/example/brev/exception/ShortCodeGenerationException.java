package org.example.brev.exception;

/**
 * Exception thrown when unable to generate a unique short code after maximum retry attempts
 */
public class ShortCodeGenerationException extends UrlServiceException {

    private final Integer maxAttempts;

    /**
     * Constructor with max attempts
     */
    public ShortCodeGenerationException(int maxAttempts) {
        super("Unable to generate unique short code after " + maxAttempts + " attempts");
        this.maxAttempts = maxAttempts;
    }

    /**
     * Constructor with custom message
     */
    public ShortCodeGenerationException(String message) {
        super(message);
        this.maxAttempts = null;
    }

    /**
     * Constructor with custom message and max attempts
     */
    public ShortCodeGenerationException(String message, int maxAttempts) {
        super(message);
        this.maxAttempts = maxAttempts;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }
}
