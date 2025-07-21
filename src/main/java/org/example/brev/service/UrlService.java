package org.example.brev.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.brev.entity.UrlMapping;
import org.example.brev.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class UrlService {

    private static final Logger logger = LogManager.getLogger(UrlService.class);
    private static final Logger auditLogger = LogManager.getLogger("org.example.brev.audit");

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SHORT_CODE_LENGTH = 6;
    private static final int MAX_RETRY_ATTEMPTS = 5;

    private final UrlMappingRepository urlMappingRepository;
    private final SecureRandom secureRandom;

    public UrlService(final UrlMappingRepository urlMappingRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Creates a short URL for the given long URL
     *
     * @param longUrl The original URL to be shortened
     * @return UrlMapping entity with the generated short code
     * @throws IllegalArgumentException if the URL is invalid
     * @throws RuntimeException if unable to generate unique short code after max attempts
     */
    public UrlMapping createShortUrl(String longUrl) {
        logger.info("Creating short URL for: {}", longUrl);

        // Validate input
        if (longUrl == null || longUrl.trim().isEmpty()) {
            logger.warn("Attempt to create short URL with null or empty long URL");
            throw new IllegalArgumentException("Long URL cannot be null or empty");
        }

        // Normalize URL (ensure it has protocol)
        String normalizedUrl = normalizeUrl(longUrl.trim());
        logger.debug("Normalized URL from '{}' to '{}'", longUrl, normalizedUrl);

        // Check if URL already exists
        Optional<UrlMapping> existingMapping = urlMappingRepository.findByLongUrl(normalizedUrl);
        if (existingMapping.isPresent()) {
            logger.info("Found existing mapping for URL: {}, returning short code: {}",
                       normalizedUrl, existingMapping.get().getShortCode());
            auditLogger.info("URL_RETRIEVAL_EXISTING - URL: {}, ShortCode: {}",
                           normalizedUrl, existingMapping.get().getShortCode());
            return existingMapping.get();
        }

        // Generate unique short code
        String shortCode = generateUniqueShortCode();
        logger.debug("Generated unique short code: {}", shortCode);

        // Create and save new URL mapping
        UrlMapping urlMapping = new UrlMapping(normalizedUrl, shortCode);
        UrlMapping savedMapping = urlMappingRepository.save(urlMapping);

        logger.info("Successfully created short URL mapping - Long URL: {}, Short Code: {}, ID: {}",
                   normalizedUrl, shortCode, savedMapping.getId());
        auditLogger.info("URL_CREATION - URL: {}, ShortCode: {}, ID: {}, Timestamp: {}",
                        normalizedUrl, shortCode, savedMapping.getId(), savedMapping.getCreatedAt());

        return savedMapping;
    }

    /**
     * Retrieves the original long URL using the short code
     *
     * @param shortCode The short code to look up
     * @return The original long URL
     * @throws IllegalArgumentException if short code is invalid
     * @throws RuntimeException if short code is not found
     */
    @Transactional(readOnly = true)
    public String getLongUrl(String shortCode) {
        logger.debug("Retrieving long URL for short code: {}", shortCode);

        // Validate input
        if (shortCode == null || shortCode.trim().isEmpty()) {
            logger.warn("Attempt to retrieve long URL with null or empty short code");
            throw new IllegalArgumentException("Short code cannot be null or empty");
        }

        String trimmedShortCode = shortCode.trim();

        // Find URL mapping
        Optional<UrlMapping> urlMapping = urlMappingRepository.findByShortCode(trimmedShortCode);

        if (urlMapping.isEmpty()) {
            logger.warn("Short code not found: {}", trimmedShortCode);
            auditLogger.warn("URL_LOOKUP_FAILED - ShortCode: {}", trimmedShortCode);
            throw new RuntimeException("Short code not found: " + shortCode);
        }

        String longUrl = urlMapping.get().getLongUrl();
        logger.info("Successfully retrieved long URL for short code: {} -> {}", trimmedShortCode, longUrl);
        auditLogger.info("URL_LOOKUP_SUCCESS - ShortCode: {}, URL: {}", trimmedShortCode, longUrl);

        return longUrl;
    }

    /**
     * Retrieves the complete URL mapping using the short code
     *
     * @param shortCode The short code to look up
     * @return Optional containing the UrlMapping if found
     */
    @Transactional(readOnly = true)
    public Optional<UrlMapping> getUrlMapping(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            return Optional.empty();
        }

        return urlMappingRepository.findByShortCode(shortCode.trim());
    }

    /**
     * Checks if a short code exists
     *
     * @param shortCode The short code to check
     * @return true if the short code exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean shortCodeExists(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            return false;
        }

        return urlMappingRepository.existsByShortCode(shortCode.trim());
    }

    /**
     * Gets statistics about URL mappings
     *
     * @return total count of URL mappings
     */
    @Transactional(readOnly = true)
    public long getTotalMappingsCount() {
        return urlMappingRepository.count();
    }

    /**
     * Deletes URL mappings older than the specified date
     *
     * @param cutoffDate The date before which mappings should be deleted
     */
    public void deleteOldMappings(LocalDateTime cutoffDate) {
        if (cutoffDate != null) {
            logger.info("Deleting URL mappings older than: {}", cutoffDate);
            long countBefore = urlMappingRepository.count();
            urlMappingRepository.deleteByCreatedAtBefore(cutoffDate);
            long countAfter = urlMappingRepository.count();
            long deletedCount = countBefore - countAfter;
            logger.info("Successfully deleted {} URL mappings older than {}", deletedCount, cutoffDate);
            auditLogger.info("URL_CLEANUP - DeletedCount: {}, CutoffDate: {}", deletedCount, cutoffDate);
        } else {
            logger.warn("Attempted to delete old mappings with null cutoff date");
        }
    }

    /**
     * Generates a unique short code
     *
     * @return A unique short code
     * @throws RuntimeException if unable to generate unique code after max attempts
     */
    private String generateUniqueShortCode() {
        logger.debug("Generating unique short code");

        for (int attempt = 0; attempt < MAX_RETRY_ATTEMPTS; attempt++) {
            String shortCode = generateRandomShortCode();

            if (!urlMappingRepository.existsByShortCode(shortCode)) {
                logger.debug("Generated unique short code '{}' on attempt {}", shortCode, attempt + 1);
                return shortCode;
            }

            logger.debug("Short code collision detected for '{}' on attempt {}", shortCode, attempt + 1);
        }

        logger.error("Failed to generate unique short code after {} attempts", MAX_RETRY_ATTEMPTS);
        throw new RuntimeException("Unable to generate unique short code after " + MAX_RETRY_ATTEMPTS + " attempts");
    }

    /**
     * Generates a random short code
     *
     * @return A random alphanumeric short code
     */
    private String generateRandomShortCode() {
        StringBuilder shortCode = new StringBuilder(SHORT_CODE_LENGTH);

        for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(CHARACTERS.length());
            shortCode.append(CHARACTERS.charAt(randomIndex));
        }

        return shortCode.toString();
    }

    /**
     * Normalizes the URL by ensuring it has a proper protocol
     *
     * @param url The URL to normalize
     * @return The normalized URL
     */
    private String normalizeUrl(String url) {
        // Convert to lowercase for protocol checking
        String lowerUrl = url.toLowerCase();

        // Add https:// if no protocol is present
        if (!lowerUrl.startsWith("http://") && !lowerUrl.startsWith("https://")) {
            return "https://" + url;
        }

        return url;
    }
}
