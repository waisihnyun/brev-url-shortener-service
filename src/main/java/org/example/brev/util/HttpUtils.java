package org.example.brev.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for HTTP-related operations
 */
public class HttpUtils {

    private HttpUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Extracts the client IP address from the HTTP request
     *
     * This method checks various headers in order of preference:
     * 1. X-Forwarded-For (common in load balancers and proxies)
     * 2. X-Real-IP (used by some reverse proxies)
     * 3. RemoteAddr (direct connection IP)
     *
     * @param request The HTTP servlet request
     * @return The client IP address
     */
    public static String getClientIpAddress(HttpServletRequest request) {
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
