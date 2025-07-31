package org.example.brev.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.example.brev.service.ScheduledMaintenanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin controller for managing scheduled tasks and viewing statistics
 * Provides endpoints for manual task execution and monitoring
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final Logger logger = LogManager.getLogger(AdminController.class);
    private static final Logger auditLogger = LogManager.getLogger("org.example.brev.audit");

    private final ScheduledMaintenanceService scheduledMaintenanceService;

    public AdminController(ScheduledMaintenanceService scheduledMaintenanceService) {
        this.scheduledMaintenanceService = scheduledMaintenanceService;
    }

    /**
     * Get current application statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        try {
            logger.info("Admin statistics request received");
            auditLogger.info("ADMIN_STATS_REQUEST - Endpoint: /admin/statistics");

            String statistics = scheduledMaintenanceService.getCurrentStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("statistics", statistics);
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error retrieving statistics: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve statistics");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger statistics collection
     */
    @PostMapping("/tasks/statistics")
    public ResponseEntity<Map<String, Object>> triggerStatistics() {
        try {
            logger.info("Manual statistics collection triggered via admin endpoint");
            auditLogger.info("ADMIN_MANUAL_STATS - Triggered by admin endpoint");

            scheduledMaintenanceService.triggerStatisticsCollection();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Statistics collection triggered successfully");
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error triggering statistics collection: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to trigger statistics collection");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Manually trigger cleanup operation
     */
    @PostMapping("/tasks/cleanup")
    public ResponseEntity<Map<String, Object>> triggerCleanup() {
        try {
            logger.info("Manual cleanup operation triggered via admin endpoint");
            auditLogger.info("ADMIN_MANUAL_CLEANUP - Triggered by admin endpoint");

            scheduledMaintenanceService.triggerCleanup();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Cleanup operation triggered successfully");
            response.put("timestamp", java.time.LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error triggering cleanup operation: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to trigger cleanup operation");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get information about scheduled tasks
     */
    @GetMapping("/tasks/info")
    public ResponseEntity<Map<String, Object>> getTasksInfo() {
        try {
            Map<String, Object> tasksInfo = new HashMap<>();
            
            // Statistics task info
            Map<String, Object> statsTask = new HashMap<>();
            statsTask.put("name", "Statistics Collection");
            statsTask.put("schedule", "Every hour (0 0 * * * *)");
            statsTask.put("description", "Logs application statistics including total links and system health");
            
            // Cleanup task info
            Map<String, Object> cleanupTask = new HashMap<>();
            cleanupTask.put("name", "URL Cleanup");
            cleanupTask.put("schedule", "Daily at 2:00 AM (0 0 2 * * *)");
            cleanupTask.put("description", "Removes old URL mappings based on retention policy");
            
            // Health check task info
            Map<String, Object> healthTask = new HashMap<>();
            healthTask.put("name", "Redis Health Check");
            healthTask.put("schedule", "Every 30 minutes");
            healthTask.put("description", "Monitors Redis connectivity and logs status");
            
            // Heartbeat task info
            Map<String, Object> heartbeatTask = new HashMap<>();
            heartbeatTask.put("name", "Application Heartbeat");
            heartbeatTask.put("schedule", "Every 15 minutes");
            heartbeatTask.put("description", "Logs application liveness for monitoring");
            
            // Weekly summary task info
            Map<String, Object> summaryTask = new HashMap<>();
            summaryTask.put("name", "Weekly Summary");
            summaryTask.put("schedule", "Sundays at 3:00 AM (0 0 3 * * SUN)");
            summaryTask.put("description", "Generates comprehensive weekly usage report");
            
            tasksInfo.put("scheduledTasks", Map.of(
                "statistics", statsTask,
                "cleanup", cleanupTask,
                "healthCheck", healthTask,
                "heartbeat", heartbeatTask,
                "weeklySummary", summaryTask
            ));
            
            tasksInfo.put("status", "success");
            tasksInfo.put("timestamp", java.time.LocalDateTime.now());
            
            return ResponseEntity.ok(tasksInfo);
        } catch (Exception e) {
            logger.error("Error retrieving tasks information: {}", e.getMessage(), e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to retrieve tasks information");
            errorResponse.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
