package org.example.brev.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.brev.exception.ShortCodeNotFoundException;
import org.example.brev.service.UrlService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/**
 * Controller for handling short URL redirects at the root level
 */
@RestController
public class RedirectController {

    private static final Logger logger = LogManager.getLogger(RedirectController.class);
    private static final Logger auditLogger = LogManager.getLogger("org.example.brev.audit");

    private final UrlService urlService;

    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    /**
     * Redirects to the original URL using the short code
     * GET /{shortCode}
     *
     * @param shortCode The short code to resolve
     * @param httpRequest The HTTP servlet request for logging
     * @return ResponseEntity with redirect to the original URL
     */
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirectToLongUrl(
            @PathVariable String shortCode,
            HttpServletRequest httpRequest) {

        logger.info("Received redirect request for short code: {}", shortCode);
        auditLogger.info("URL_REDIRECT_REQUEST - IP: {}, ShortCode: {}",
                        getClientIpAddress(httpRequest), shortCode);

        try {
            // Get the long URL from the service
            String longUrl = urlService.getLongUrl(shortCode);

            logger.info("Redirecting {} to {}", shortCode, longUrl);
            auditLogger.info("URL_REDIRECT_SUCCESS - IP: {}, ShortCode: {}, URL: {}",
                           getClientIpAddress(httpRequest), shortCode, longUrl);

            // Return 302 redirect response
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(longUrl))
                    .build();

        } catch (ShortCodeNotFoundException e) {
            logger.warn("Short code not found: {}", shortCode);
            auditLogger.warn("URL_REDIRECT_NOT_FOUND - IP: {}, ShortCode: {}",
                           getClientIpAddress(httpRequest), shortCode);
            throw e; // Will be handled by global exception handler
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid short code format: {}", shortCode);
            auditLogger.warn("URL_REDIRECT_INVALID - IP: {}, ShortCode: {}, Error: {}",
                           getClientIpAddress(httpRequest), shortCode, e.getMessage());
            throw e; // Will be handled by global exception handler
        } catch (Exception e) {
            logger.error("Unexpected error during redirect for short code: {}", shortCode, e);
            auditLogger.error("URL_REDIRECT_ERROR - IP: {}, ShortCode: {}, Error: {}",
                            getClientIpAddress(httpRequest), shortCode, e.getMessage());
            throw e; // Will be handled by global exception handler
        }
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
