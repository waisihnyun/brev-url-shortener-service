package org.example.brev.dto;

import java.time.LocalDateTime;

/**
 * Response DTO for short URL creation
 */
public class CreateUrlResponse {

    private Long id;
    private String longUrl;
    private String shortCode;
    private String shortUrl;
    private LocalDateTime createdAt;

    // Default constructor
    public CreateUrlResponse() {
    }

    // Constructor
    public CreateUrlResponse(Long id, String longUrl, String shortCode, String shortUrl, LocalDateTime createdAt) {
        this.id = id;
        this.longUrl = longUrl;
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLongUrl() {
        return longUrl;
    }

    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
