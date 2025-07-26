package org.example.brev.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.brev.dto.CreateUrlRequest;
import org.example.brev.dto.CreateUrlResponse;
import org.example.brev.entity.UrlMapping;
import org.example.brev.exception.ShortCodeNotFoundException;
import org.example.brev.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * REST Controller for URL shortening operations
 */
@RestController
@RequestMapping("/api/v1")
public class UrlController {

    private static final Logger logger = LogManager.getLogger(UrlController.class);
    private static final Logger auditLogger = LogManager.getLogger("org.example.brev.audit");

    private final UrlService urlService;

    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Creates a short URL for the given long URL
     * POST /api/v1/urls
     *
     * @param request The request containing the long URL
     * @param httpRequest The HTTP servlet request for building the short URL
     * @return ResponseEntity containing the created short URL information
     */
    @PostMapping("/urls")
    public ResponseEntity<CreateUrlResponse> createShortUrl(
            @Valid @RequestBody CreateUrlRequest request,
            HttpServletRequest httpRequest) {

        logger.info("Received request to create short URL for: {}", request.getLongUrl());
        auditLogger.info("URL_CREATION_REQUEST - IP: {}, URL: {}",
                        getClientIpAddress(httpRequest), request.getLongUrl());

        try {
            // Create the short URL using the service
            UrlMapping urlMapping = urlService.createShortUrl(request.getLongUrl());

            // Build the complete short URL
            String baseUrl = getBaseUrl(httpRequest);
            String shortUrl = baseUrl + "/" + urlMapping.getShortCode();

            // Create response
            CreateUrlResponse response = new CreateUrlResponse(
                    urlMapping.getId(),
                    urlMapping.getLongUrl(),
                    urlMapping.getShortCode(),
                    shortUrl,
                    urlMapping.getCreatedAt()
            );

            logger.info("Successfully created short URL: {} -> {}", request.getLongUrl(), shortUrl);
            auditLogger.info("URL_CREATION_SUCCESS - IP: {}, URL: {}, ShortCode: {}, ShortUrl: {}",
                           getClientIpAddress(httpRequest), request.getLongUrl(),
                           urlMapping.getShortCode(), shortUrl);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid request for URL creation: {}", e.getMessage());
            auditLogger.warn("URL_CREATION_INVALID - IP: {}, URL: {}, Error: {}",
                           getClientIpAddress(httpRequest), request.getLongUrl(), e.getMessage());
            throw e; // Will be handled by global exception handler
        } catch (HttpMessageNotReadableException e) {
            logger.error("Malformed request body for URL creation: {}", e.getMessage());
            auditLogger.error("URL_CREATION_MALFORMED - IP: {}, URL: {}, Error: {}",
                            getClientIpAddress(httpRequest), request.getLongUrl(), e.getMessage());
            throw new IllegalArgumentException("Malformed request body", e); // Will be handled by global exception handler
        } catch (Exception e) {
            logger.error("Unexpected error during URL creation for: {}", request.getLongUrl(), e);
            auditLogger.error("URL_CREATION_ERROR - IP: {}, URL: {}, Error: {}",
                            getClientIpAddress(httpRequest), request.getLongUrl(), e.getMessage());
            throw e; // Will be handled by global exception handler
        }
    }

    /**
     * Builds the base URL from the HTTP request (root level for short URLs)
     *
     * @param request The HTTP servlet request
     * @return The base URL (scheme + server + port + context path)
     */
    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        StringBuilder baseUrl = new StringBuilder();
        baseUrl.append(scheme).append("://").append(serverName);

        // Only append port if it's not the default port for the scheme
        if ((scheme.equals("http") && serverPort != 80) ||
            (scheme.equals("https") && serverPort != 443)) {
            baseUrl.append(":").append(serverPort);
        }

        baseUrl.append(contextPath);
        return baseUrl.toString();
    }

    /**
     * Extracts the client IP address from the HTTP request
     *
     * @param request The HTTP servlet request
     * @return The client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}
