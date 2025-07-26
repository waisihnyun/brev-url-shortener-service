package org.example.brev.dto;

import org.example.brev.validation.ValidLongUrl;

/**
 * Request DTO for creating a short URL
 */
public class CreateUrlRequest {

    @ValidLongUrl
    private String longUrl;

    // Default constructor
    public CreateUrlRequest() {
    }

    // Constructor
    public CreateUrlRequest(String longUrl) {
        this.longUrl = longUrl;
    }

    // Getters and Setters
    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }
}
