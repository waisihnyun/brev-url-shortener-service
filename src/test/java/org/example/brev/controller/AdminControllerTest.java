package org.example.brev.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.brev.service.ScheduledMaintenanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminController.class)
@DisplayName("AdminController Tests")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScheduledMaintenanceService scheduledMaintenanceService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ADMIN_BASE_URL = "/api/v1/admin";

    @BeforeEach
    void setUp() {
        reset(scheduledMaintenanceService);
    }

    @Nested
    @DisplayName("Get Statistics Tests")
    class GetStatisticsTests {

        @Test
        @DisplayName("Should return statistics successfully")
        void shouldReturnStatisticsSuccessfully() throws Exception {
            // Given
            String mockStatistics = "Total URLs: 150, Active URLs: 120, Cache Hit Rate: 85%";
            when(scheduledMaintenanceService.getCurrentStatistics()).thenReturn(mockStatistics);

            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.statistics").value(mockStatistics))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());

            verify(scheduledMaintenanceService, times(1)).getCurrentStatistics();
        }

        @Test
        @DisplayName("Should handle service exception gracefully")
        void shouldHandleServiceExceptionGracefully() throws Exception {
            // Given
            String errorMessage = "Database connection failed";
            when(scheduledMaintenanceService.getCurrentStatistics())
                    .thenThrow(new RuntimeException(errorMessage));

            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve statistics"))
                    .andExpect(jsonPath("$.error").value(errorMessage));

            verify(scheduledMaintenanceService, times(1)).getCurrentStatistics();
        }

        @Test
        @DisplayName("Should handle null statistics gracefully")
        void shouldHandleNullStatisticsGracefully() throws Exception {
            // Given
            when(scheduledMaintenanceService.getCurrentStatistics()).thenReturn(null);

            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.statistics").isEmpty())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(scheduledMaintenanceService, times(1)).getCurrentStatistics();
        }
    }

    @Nested
    @DisplayName("Trigger Statistics Tests")
    class TriggerStatisticsTests {

        @Test
        @DisplayName("Should trigger statistics collection successfully")
        void shouldTriggerStatisticsCollectionSuccessfully() throws Exception {
            // Given
            doNothing().when(scheduledMaintenanceService).triggerStatisticsCollection();

            // When & Then
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Statistics collection triggered successfully"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());

            verify(scheduledMaintenanceService, times(1)).triggerStatisticsCollection();
        }

        @Test
        @DisplayName("Should handle service exception during statistics trigger")
        void shouldHandleServiceExceptionDuringStatisticsTrigger() throws Exception {
            // Given
            String errorMessage = "Statistics service unavailable";
            doThrow(new RuntimeException(errorMessage))
                    .when(scheduledMaintenanceService).triggerStatisticsCollection();

            // When & Then
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to trigger statistics collection"))
                    .andExpect(jsonPath("$.error").value(errorMessage));

            verify(scheduledMaintenanceService, times(1)).triggerStatisticsCollection();
        }
    }

    @Nested
    @DisplayName("Trigger Cleanup Tests")
    class TriggerCleanupTests {

        @Test
        @DisplayName("Should trigger cleanup operation successfully")
        void shouldTriggerCleanupOperationSuccessfully() throws Exception {
            // Given
            doNothing().when(scheduledMaintenanceService).triggerCleanup();

            // When & Then
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").value("Cleanup operation triggered successfully"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.timestamp").isNotEmpty());

            verify(scheduledMaintenanceService, times(1)).triggerCleanup();
        }

        @Test
        @DisplayName("Should handle service exception during cleanup trigger")
        void shouldHandleServiceExceptionDuringCleanupTrigger() throws Exception {
            // Given
            String errorMessage = "Cleanup service failed";
            doThrow(new RuntimeException(errorMessage))
                    .when(scheduledMaintenanceService).triggerCleanup();

            // When & Then
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to trigger cleanup operation"))
                    .andExpect(jsonPath("$.error").value(errorMessage));

            verify(scheduledMaintenanceService, times(1)).triggerCleanup();
        }
    }

    @Nested
    @DisplayName("Get Tasks Info Tests")
    class GetTasksInfoTests {

        @Test
        @DisplayName("Should return tasks information successfully")
        void shouldReturnTasksInformationSuccessfully() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.scheduledTasks").exists())
                    .andExpect(jsonPath("$.scheduledTasks.statistics").exists())
                    .andExpect(jsonPath("$.scheduledTasks.cleanup").exists())
                    .andExpect(jsonPath("$.scheduledTasks.healthCheck").exists())
                    .andExpect(jsonPath("$.scheduledTasks.heartbeat").exists())
                    .andExpect(jsonPath("$.scheduledTasks.weeklySummary").exists());

            verifyNoInteractions(scheduledMaintenanceService);
        }

        @Test
        @DisplayName("Should return correct statistics task information")
        void shouldReturnCorrectStatisticsTaskInformation() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledTasks.statistics.name").value("Statistics Collection"))
                    .andExpect(jsonPath("$.scheduledTasks.statistics.schedule").value("Every hour (0 0 * * * *)"))
                    .andExpect(jsonPath("$.scheduledTasks.statistics.description")
                            .value("Logs application statistics including total links and system health"));
        }

        @Test
        @DisplayName("Should return correct cleanup task information")
        void shouldReturnCorrectCleanupTaskInformation() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledTasks.cleanup.name").value("URL Cleanup"))
                    .andExpect(jsonPath("$.scheduledTasks.cleanup.schedule").value("Daily at 2:00 AM (0 0 2 * * *)"))
                    .andExpect(jsonPath("$.scheduledTasks.cleanup.description")
                            .value("Removes old URL mappings based on retention policy"));
        }

        @Test
        @DisplayName("Should return correct health check task information")
        void shouldReturnCorrectHealthCheckTaskInformation() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledTasks.healthCheck.name").value("Redis Health Check"))
                    .andExpect(jsonPath("$.scheduledTasks.healthCheck.schedule").value("Every 30 minutes"))
                    .andExpect(jsonPath("$.scheduledTasks.healthCheck.description")
                            .value("Monitors Redis connectivity and logs status"));
        }

        @Test
        @DisplayName("Should return correct heartbeat task information")
        void shouldReturnCorrectHeartbeatTaskInformation() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledTasks.heartbeat.name").value("Application Heartbeat"))
                    .andExpect(jsonPath("$.scheduledTasks.heartbeat.schedule").value("Every 15 minutes"))
                    .andExpect(jsonPath("$.scheduledTasks.heartbeat.description")
                            .value("Logs application liveness for monitoring"));
        }

        @Test
        @DisplayName("Should return correct weekly summary task information")
        void shouldReturnCorrectWeeklySummaryTaskInformation() throws Exception {
            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scheduledTasks.weeklySummary.name").value("Weekly Summary"))
                    .andExpect(jsonPath("$.scheduledTasks.weeklySummary.schedule")
                            .value("Sundays at 3:00 AM (0 0 3 * * SUN)"))
                    .andExpect(jsonPath("$.scheduledTasks.weeklySummary.description")
                            .value("Generates comprehensive weekly usage report"));
        }
    }

    @Nested
    @DisplayName("HTTP Method Tests")
    class HttpMethodTests {

        @Test
        @DisplayName("Should reject POST request to statistics endpoint")
        void shouldRejectPostRequestToStatisticsEndpoint() throws Exception {
            mockMvc.perform(post(ADMIN_BASE_URL + "/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should reject GET request to statistics trigger endpoint")
        void shouldRejectGetRequestToStatisticsTriggerEndpoint() throws Exception {
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/statistics")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should reject GET request to cleanup trigger endpoint")
        void shouldRejectGetRequestToCleanupTriggerEndpoint() throws Exception {
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/cleanup")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("Should reject POST request to tasks info endpoint")
        void shouldRejectPostRequestToTasksInfoEndpoint() throws Exception {
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/info")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    @Nested
    @DisplayName("Content Type Tests")
    class ContentTypeTests {

        @Test
        @DisplayName("Should handle requests without explicit content type")
        void shouldHandleRequestsWithoutExplicitContentType() throws Exception {
            // Given
            String mockStatistics = "Test statistics";
            when(scheduledMaintenanceService.getCurrentStatistics()).thenReturn(mockStatistics);

            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.statistics").value(mockStatistics));
        }

        @Test
        @DisplayName("Should return JSON content type for all endpoints")
        void shouldReturnJsonContentTypeForAllEndpoints() throws Exception {
            // Given
            when(scheduledMaintenanceService.getCurrentStatistics()).thenReturn("test");
            doNothing().when(scheduledMaintenanceService).triggerStatisticsCollection();
            doNothing().when(scheduledMaintenanceService).triggerCleanup();

            // Test all endpoints return JSON
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/statistics"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle null pointer exception in statistics")
        void shouldHandleNullPointerExceptionInStatistics() throws Exception {
            // Given
            when(scheduledMaintenanceService.getCurrentStatistics())
                    .thenThrow(new NullPointerException("Null reference"));

            // When & Then
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to retrieve statistics"))
                    .andExpect(jsonPath("$.error").value("Null reference"));
        }

        @Test
        @DisplayName("Should handle illegal state exception in cleanup")
        void shouldHandleIllegalStateExceptionInCleanup() throws Exception {
            // Given
            doThrow(new IllegalStateException("Invalid cleanup state"))
                    .when(scheduledMaintenanceService).triggerCleanup();

            // When & Then
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").value("Failed to trigger cleanup operation"))
                    .andExpect(jsonPath("$.error").value("Invalid cleanup state"));
        }
    }

    @Nested
    @DisplayName("Response Structure Tests")
    class ResponseStructureTests {

        @Test
        @DisplayName("Should have consistent response structure for success cases")
        void shouldHaveConsistentResponseStructureForSuccessCases() throws Exception {
            // Given
            when(scheduledMaintenanceService.getCurrentStatistics()).thenReturn("test");
            doNothing().when(scheduledMaintenanceService).triggerStatisticsCollection();
            doNothing().when(scheduledMaintenanceService).triggerCleanup();

            // Test statistics endpoint
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics"))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.timestamp").exists());

            // Test statistics trigger endpoint
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/statistics"))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            // Test cleanup trigger endpoint
            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup"))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            // Test tasks info endpoint
            mockMvc.perform(get(ADMIN_BASE_URL + "/tasks/info"))
                    .andExpect(jsonPath("$.status").value("success"))
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("Should have consistent error response structure")
        void shouldHaveConsistentErrorResponseStructure() throws Exception {
            // Given
            when(scheduledMaintenanceService.getCurrentStatistics())
                    .thenThrow(new RuntimeException("Test error"));
            doThrow(new RuntimeException("Test error"))
                    .when(scheduledMaintenanceService).triggerStatisticsCollection();
            doThrow(new RuntimeException("Test error"))
                    .when(scheduledMaintenanceService).triggerCleanup();

            // Test all error responses have consistent structure
            mockMvc.perform(get(ADMIN_BASE_URL + "/statistics"))
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists());

            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/statistics"))
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists());

            mockMvc.perform(post(ADMIN_BASE_URL + "/tasks/cleanup"))
                    .andExpect(jsonPath("$.status").value("error"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.error").exists());
        }
    }
}
