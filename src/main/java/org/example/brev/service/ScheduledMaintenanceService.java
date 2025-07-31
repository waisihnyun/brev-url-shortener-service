package org.example.brev.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Scheduled service for performing periodic maintenance tasks
 * including statistics logging and cleanup operations
 */
@Service
public class ScheduledMaintenanceService {

    private static final Logger logger = LogManager.getLogger(ScheduledMaintenanceService.class);
    private static final Logger auditLogger = LogManager.getLogger("org.example.brev.audit");
    private static final Logger statsLogger = LogManager.getLogger("org.example.brev.stats");
    
    private final UrlService urlService;
    private final RedisCacheService redisCacheService;

    @Value("${app.cleanup.retention-days:30}")
    private int retentionDays;

    @Value("${app.statistics.enabled:true}")
    private boolean statisticsEnabled;

    @Value("${app.cleanup.enabled:true}")
    private boolean cleanupEnabled;

    public ScheduledMaintenanceService(UrlService urlService, RedisCacheService redisCacheService) {
        this.urlService = urlService;
        this.redisCacheService = redisCacheService;
    }

    /**
     * Log application statistics every hour
     * Runs at the top of every hour (e.g., 1:00, 2:00, 3:00, etc.)
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logApplicationStatistics() {
        if (!statisticsEnabled) {
            logger.debug("Statistics logging is disabled");
            return;
        }

        try {
            logger.info("Starting periodic statistics collection...");
            
            long totalLinks = urlService.getTotalMappingsCount();
            boolean redisHealthy = redisCacheService.isRedisAvailable();
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            
            // Log to both regular log and stats-specific log
            String statsMessage = String.format(
                "PERIODIC_STATS - Timestamp: %s, Total links: %d, Redis healthy: %s",
                timestamp, totalLinks, redisHealthy
            );
            
            logger.info("Application Statistics - Total links created: {}, Redis status: {}", 
                       totalLinks, redisHealthy ? "HEALTHY" : "UNHEALTHY");
            
            statsLogger.info(statsMessage);
            auditLogger.info("SCHEDULED_STATS_COLLECTION - TotalLinks: {}, RedisStatus: {}", 
                           totalLinks, redisHealthy ? "HEALTHY" : "UNHEALTHY");

            // Log additional runtime information
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();

            logger.info("Memory Usage - Used: {} MB, Free: {} MB, Total: {} MB, Max: {} MB",
                       usedMemory / (1024 * 1024),
                       freeMemory / (1024 * 1024),
                       totalMemory / (1024 * 1024),
                       maxMemory / (1024 * 1024));

            statsLogger.info("MEMORY_STATS - Used: {}MB, Free: {}MB, Total: {}MB, Max: {}MB",
                            usedMemory / (1024 * 1024),
                            freeMemory / (1024 * 1024),
                            totalMemory / (1024 * 1024),
                            maxMemory / (1024 * 1024));

        } catch (Exception e) {
            logger.error("Error occurred during statistics collection: {}", e.getMessage(), e);
            auditLogger.error("SCHEDULED_STATS_ERROR - Error: {}", e.getMessage());
        }
    }

    /**
     * Clean up old URL mappings daily at 2:00 AM
     * Removes URLs older than the configured retention period
     */
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldUrlMappings() {
        if (!cleanupEnabled) {
            logger.debug("Cleanup is disabled");
            return;
        }

        try {
            logger.info("Starting scheduled cleanup of old URL mappings older than {} days...", retentionDays);
            
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            long countBefore = urlService.getTotalMappingsCount();
            
            urlService.deleteOldMappings(cutoffDate);
            
            long countAfter = urlService.getTotalMappingsCount();
            long deletedCount = countBefore - countAfter;
            
            logger.info("Cleanup completed - Deleted {} old URL mappings (cutoff date: {})", 
                       deletedCount, cutoffDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            auditLogger.info("SCHEDULED_CLEANUP_COMPLETED - DeletedCount: {}, CutoffDate: {}, RetentionDays: {}", 
                           deletedCount, cutoffDate, retentionDays);
            
            statsLogger.info("CLEANUP_STATS - Before: {}, After: {}, Deleted: {}, RetentionDays: {}", 
                            countBefore, countAfter, deletedCount, retentionDays);

        } catch (Exception e) {
            logger.error("Error occurred during scheduled cleanup: {}", e.getMessage(), e);
            auditLogger.error("SCHEDULED_CLEANUP_ERROR - Error: {}", e.getMessage());
        }
    }

    /**
     * Perform Redis health check every 30 minutes
     * Logs Redis connectivity status for monitoring
     */
    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.MINUTES)
    public void performRedisHealthCheck() {
        try {
            boolean isHealthy = redisCacheService.isRedisAvailable();
            
            if (isHealthy) {
                logger.debug("Redis health check passed");
                auditLogger.debug("REDIS_HEALTH_CHECK - Status: HEALTHY");
            } else {
                logger.warn("Redis health check failed - Cache may be unavailable");
                auditLogger.warn("REDIS_HEALTH_CHECK - Status: UNHEALTHY");
                
                // Optionally, you could trigger alerts here
                statsLogger.warn("REDIS_UNAVAILABLE - Timestamp: {}", 
                                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
        } catch (Exception e) {
            logger.error("Error during Redis health check: {}", e.getMessage(), e);
            auditLogger.error("REDIS_HEALTH_CHECK_ERROR - Error: {}", e.getMessage());
        }
    }

    /**
     * Log periodic application heartbeat every 15 minutes
     * Useful for monitoring application liveness
     */
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void applicationHeartbeat() {
        logger.debug("Application heartbeat - System running normally");
        statsLogger.debug("HEARTBEAT - Timestamp: {}, Status: RUNNING", 
                         LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }

    /**
     * Weekly summary report - runs every Sunday at 3:00 AM
     * Provides a comprehensive weekly summary of application usage
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    public void generateWeeklySummaryReport() {
        if (!statisticsEnabled) {
            logger.debug("Weekly summary reporting is disabled");
            return;
        }

        try {
            logger.info("Generating weekly summary report...");
            
            long totalLinks = urlService.getTotalMappingsCount();
            LocalDateTime reportDate = LocalDateTime.now();
            LocalDateTime weekStart = reportDate.minusDays(7);
            
            String weeklyReport = String.format(
                "WEEKLY_SUMMARY - Week ending %s: Total links in system: %d, Report generated: %s",
                reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                totalLinks,
                reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
            
            logger.info("Weekly Summary Report - Total links: {} (as of {})", 
                       totalLinks, reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            statsLogger.info(weeklyReport);
            auditLogger.info("WEEKLY_REPORT_GENERATED - TotalLinks: {}, ReportDate: {}", 
                           totalLinks, reportDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
        } catch (Exception e) {
            logger.error("Error generating weekly summary report: {}", e.getMessage(), e);
            auditLogger.error("WEEKLY_REPORT_ERROR - Error: {}", e.getMessage());
        }
    }

    /**
     * Manual trigger for statistics collection (for testing or admin purposes)
     */
    public void triggerStatisticsCollection() {
        logger.info("Manually triggered statistics collection");
        logApplicationStatistics();
    }

    /**
     * Manual trigger for cleanup (for testing or admin purposes)
     */
    public void triggerCleanup() {
        logger.info("Manually triggered cleanup operation");
        cleanupOldUrlMappings();
    }

    /**
     * Get current statistics as a formatted string
     * Useful for health checks or admin endpoints
     */
    public String getCurrentStatistics() {
        try {
            long totalLinks = urlService.getTotalMappingsCount();
            boolean redisHealthy = redisCacheService.isRedisAvailable();
            
            return String.format(
                "Current Statistics - Total Links: %d, Redis Status: %s, Last Updated: %s",
                totalLinks,
                redisHealthy ? "HEALTHY" : "UNHEALTHY",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            );
        } catch (Exception e) {
            return String.format("Error retrieving statistics: %s", e.getMessage());
        }
    }
}
