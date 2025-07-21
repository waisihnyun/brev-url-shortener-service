package org.example.brev.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "short_code", nullable = false, unique = true, length = 10)
    private String shortCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Default constructor
    public UrlMapping() {
    }

    // Constructor
    public UrlMapping(String longUrl, String shortCode) {
        this.longUrl = longUrl;
        this.shortCode = shortCode;
        this.createdAt = LocalDateTime.now();
    }
}
