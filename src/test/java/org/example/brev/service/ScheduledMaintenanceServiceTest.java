package org.example.brev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduledMaintenanceService Tests")
class ScheduledMaintenanceServiceTest {

    @Mock
    private UrlService urlService;

    @Mock
    private RedisCacheService redisCacheService;

    @InjectMocks
    private ScheduledMaintenanceService scheduledMaintenanceService;

    @BeforeEach
    void setUp() {
        // Set default configuration values
        ReflectionTestUtils.setField(scheduledMaintenanceService, "retentionDays", 30);
        ReflectionTestUtils.setField(scheduledMaintenanceService, "statisticsEnabled", true);
        ReflectionTestUtils.setField(scheduledMaintenanceService, "cleanupEnabled", true);
    }

    @Test
    @DisplayName("Should log application statistics when enabled")
    void shouldLogApplicationStatisticsWhenEnabled() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(1000L);
        when(redisCacheService.isRedisAvailable()).thenReturn(true);

        // When
        scheduledMaintenanceService.logApplicationStatistics();

        // Then
        verify(urlService).getTotalMappingsCount();
        verify(redisCacheService).isRedisAvailable();
    }

    @Test
    @DisplayName("Should skip statistics logging when disabled")
    void shouldSkipStatisticsLoggingWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(scheduledMaintenanceService, "statisticsEnabled", false);

        // When
        scheduledMaintenanceService.logApplicationStatistics();

        // Then
        verifyNoInteractions(urlService);
        verifyNoInteractions(redisCacheService);
    }

    @Test
    @DisplayName("Should handle exception during statistics collection gracefully")
    void shouldHandleExceptionDuringStatisticsCollection() {
        // Given
        when(urlService.getTotalMappingsCount()).thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        scheduledMaintenanceService.logApplicationStatistics();

        verify(urlService).getTotalMappingsCount();
    }

    @Test
    @DisplayName("Should perform cleanup when enabled")
    void shouldPerformCleanupWhenEnabled() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(500L, 450L); // before and after cleanup
        doNothing().when(urlService).deleteOldMappings(any());

        // When
        scheduledMaintenanceService.cleanupOldUrlMappings();

        // Then
        verify(urlService, times(2)).getTotalMappingsCount();
        verify(urlService).deleteOldMappings(any());
    }

    @Test
    @DisplayName("Should skip cleanup when disabled")
    void shouldSkipCleanupWhenDisabled() {
        // Given
        ReflectionTestUtils.setField(scheduledMaintenanceService, "cleanupEnabled", false);

        // When
        scheduledMaintenanceService.cleanupOldUrlMappings();

        // Then
        verifyNoInteractions(urlService);
    }

    @Test
    @DisplayName("Should handle exception during cleanup gracefully")
    void shouldHandleExceptionDuringCleanup() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(500L);
        doThrow(new RuntimeException("Cleanup failed")).when(urlService).deleteOldMappings(any());

        // When & Then - should not throw exception
        scheduledMaintenanceService.cleanupOldUrlMappings();

        verify(urlService).getTotalMappingsCount();
        verify(urlService).deleteOldMappings(any());
    }

    @Test
    @DisplayName("Should perform Redis health check")
    void shouldPerformRedisHealthCheck() {
        // Given
        when(redisCacheService.isRedisAvailable()).thenReturn(true);

        // When
        scheduledMaintenanceService.performRedisHealthCheck();

        // Then
        verify(redisCacheService).isRedisAvailable();
    }

    @Test
    @DisplayName("Should handle Redis health check failure")
    void shouldHandleRedisHealthCheckFailure() {
        // Given
        when(redisCacheService.isRedisAvailable()).thenReturn(false);

        // When
        scheduledMaintenanceService.performRedisHealthCheck();

        // Then
        verify(redisCacheService).isRedisAvailable();
    }

    @Test
    @DisplayName("Should generate weekly summary when statistics enabled")
    void shouldGenerateWeeklySummaryWhenStatisticsEnabled() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(2000L);

        // When
        scheduledMaintenanceService.generateWeeklySummaryReport();

        // Then
        verify(urlService).getTotalMappingsCount();
    }

    @Test
    @DisplayName("Should skip weekly summary when statistics disabled")
    void shouldSkipWeeklySummaryWhenStatisticsDisabled() {
        // Given
        ReflectionTestUtils.setField(scheduledMaintenanceService, "statisticsEnabled", false);

        // When
        scheduledMaintenanceService.generateWeeklySummaryReport();

        // Then
        verifyNoInteractions(urlService);
    }

    @Test
    @DisplayName("Should trigger manual statistics collection")
    void shouldTriggerManualStatisticsCollection() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(1500L);
        when(redisCacheService.isRedisAvailable()).thenReturn(true);

        // When
        scheduledMaintenanceService.triggerStatisticsCollection();

        // Then
        verify(urlService).getTotalMappingsCount();
        verify(redisCacheService).isRedisAvailable();
    }

    @Test
    @DisplayName("Should trigger manual cleanup")
    void shouldTriggerManualCleanup() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(800L, 750L);
        doNothing().when(urlService).deleteOldMappings(any());

        // When
        scheduledMaintenanceService.triggerCleanup();

        // Then
        verify(urlService, times(2)).getTotalMappingsCount();
        verify(urlService).deleteOldMappings(any());
    }

    @Test
    @DisplayName("Should return current statistics")
    void shouldReturnCurrentStatistics() {
        // Given
        when(urlService.getTotalMappingsCount()).thenReturn(1200L);
        when(redisCacheService.isRedisAvailable()).thenReturn(true);

        // When
        String statistics = scheduledMaintenanceService.getCurrentStatistics();

        // Then
        assertThat(statistics).contains("1200");
        assertThat(statistics).contains("HEALTHY");
        verify(urlService).getTotalMappingsCount();
        verify(redisCacheService).isRedisAvailable();
    }

    @Test
    @DisplayName("Should handle error when getting current statistics")
    void shouldHandleErrorWhenGettingCurrentStatistics() {
        // Given
        when(urlService.getTotalMappingsCount()).thenThrow(new RuntimeException("Service unavailable"));

        // When
        String statistics = scheduledMaintenanceService.getCurrentStatistics();

        // Then
        assertThat(statistics).contains("Error retrieving statistics");
        assertThat(statistics).contains("Service unavailable");
    }
}
