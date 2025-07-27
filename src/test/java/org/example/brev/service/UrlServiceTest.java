package org.example.brev.service;

import org.example.brev.entity.UrlMapping;
import org.example.brev.exception.ShortCodeGenerationException;
import org.example.brev.exception.ShortCodeNotFoundException;
import org.example.brev.repository.UrlMappingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UrlService Unit Tests")
class UrlServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @InjectMocks
    private UrlService urlService;

    private UrlMapping testUrlMapping;
    private final String testLongUrl = "https://example.com/very/long/url/path";
    private final String testShortCode = "abc123";
    private final String testUrlWithoutProtocol = "example.com/test";

    @BeforeEach
    void setUp() {
        testUrlMapping = new UrlMapping(testLongUrl, testShortCode);
        testUrlMapping.setId(1L);
        testUrlMapping.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createShortUrl() Tests")
    class CreateShortUrlTests {

        @Test
        @DisplayName("Should create short URL successfully for new long URL")
        void shouldCreateShortUrlSuccessfully() {
            // Given
            when(urlMappingRepository.findByLongUrl(testLongUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            UrlMapping result = urlService.createShortUrl(testLongUrl);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLongUrl()).isEqualTo(testLongUrl);
            assertThat(result.getShortCode()).isNotNull();
            verify(urlMappingRepository).findByLongUrl(testLongUrl);
            verify(urlMappingRepository).save(any(UrlMapping.class));
        }

        @Test
        @DisplayName("Should return existing mapping if URL already exists")
        void shouldReturnExistingMappingIfUrlExists() {
            // Given
            when(urlMappingRepository.findByLongUrl(testLongUrl)).thenReturn(Optional.of(testUrlMapping));

            // When
            UrlMapping result = urlService.createShortUrl(testLongUrl);

            // Then
            assertThat(result).isEqualTo(testUrlMapping);
            verify(urlMappingRepository).findByLongUrl(testLongUrl);
            verify(urlMappingRepository, never()).save(any(UrlMapping.class));
        }

        @Test
        @DisplayName("Should normalize URL by adding https protocol")
        void shouldNormalizeUrlByAddingHttpsProtocol() {
            // Given
            String expectedNormalizedUrl = "https://" + testUrlWithoutProtocol;
            when(urlMappingRepository.findByLongUrl(expectedNormalizedUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            urlService.createShortUrl(testUrlWithoutProtocol);

            // Then
            verify(urlMappingRepository).findByLongUrl(expectedNormalizedUrl);
        }

        @Test
        @DisplayName("Should throw exception for null URL")
        void shouldThrowExceptionForNullUrl() {
            // When & Then
            assertThatThrownBy(() -> urlService.createShortUrl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Long URL cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty URL")
        void shouldThrowExceptionForEmptyUrl() {
            // When & Then
            assertThatThrownBy(() -> urlService.createShortUrl("   "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Long URL cannot be null or empty");
        }

        @Test
        @DisplayName("Should handle short code collision and retry")
        void shouldHandleShortCodeCollisionAndRetry() {
            // Given
            when(urlMappingRepository.findByLongUrl(testLongUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString()))
                    .thenReturn(true)  // First attempt - collision
                    .thenReturn(false); // Second attempt - success
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            UrlMapping result = urlService.createShortUrl(testLongUrl);

            // Then
            assertThat(result).isNotNull();
            verify(urlMappingRepository, times(2)).existsByShortCode(anyString());
        }

        @Test
        @DisplayName("Should throw exception when max retry attempts exceeded")
        void shouldThrowExceptionWhenMaxRetryAttemptsExceeded() {
            // Given
            when(urlMappingRepository.findByLongUrl(testLongUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(true); // Always collision

            // When & Then
            assertThatThrownBy(() -> urlService.createShortUrl(testLongUrl))
                    .isInstanceOf(ShortCodeGenerationException.class)
                    .hasMessage("Unable to generate unique short code after 5 attempts");
        }
    }

    @Nested
    @DisplayName("getLongUrl() Tests")
    class GetLongUrlTests {

        @Test
        @DisplayName("Should return long URL for valid short code")
        void shouldReturnLongUrlForValidShortCode() {
            // Given
            when(urlMappingRepository.findByShortCode(testShortCode))
                    .thenReturn(Optional.of(testUrlMapping));

            // When
            String result = urlService.getLongUrl(testShortCode);

            // Then
            assertThat(result).isEqualTo(testLongUrl);
            verify(urlMappingRepository).findByShortCode(testShortCode);
        }

        @Test
        @DisplayName("Should throw exception for non-existent short code")
        void shouldThrowExceptionForNonExistentShortCode() {
            // Given
            String nonExistentCode = "xyz789";
            when(urlMappingRepository.findByShortCode(nonExistentCode))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> urlService.getLongUrl(nonExistentCode))
                    .isInstanceOf(ShortCodeNotFoundException.class)
                    .hasMessage(String.format("Short code '%s' not found", nonExistentCode));
        }

        @Test
        @DisplayName("Should throw exception for null short code")
        void shouldThrowExceptionForNullShortCode() {
            // When & Then
            assertThatThrownBy(() -> urlService.getLongUrl(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Short code cannot be null or empty");
        }

        @Test
        @DisplayName("Should throw exception for empty short code")
        void shouldThrowExceptionForEmptyShortCode() {
            // When & Then
            assertThatThrownBy(() -> urlService.getLongUrl(""))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Short code cannot be null or empty");
        }

        @Test
        @DisplayName("Should handle whitespace in short code")
        void shouldHandleWhitespaceInShortCode() {
            // Given
            String shortCodeWithWhitespace = "  " + testShortCode + "  ";
            when(urlMappingRepository.findByShortCode(testShortCode))
                    .thenReturn(Optional.of(testUrlMapping));

            // When
            String result = urlService.getLongUrl(shortCodeWithWhitespace);

            // Then
            assertThat(result).isEqualTo(testLongUrl);
            verify(urlMappingRepository).findByShortCode(testShortCode);
        }
    }

    @Nested
    @DisplayName("getUrlMapping() Tests")
    class GetUrlMappingTests {

        @Test
        @DisplayName("Should return URL mapping for valid short code")
        void shouldReturnUrlMappingForValidShortCode() {
            // Given
            when(urlMappingRepository.findByShortCode(testShortCode))
                    .thenReturn(Optional.of(testUrlMapping));

            // When
            Optional<UrlMapping> result = urlService.getUrlMapping(testShortCode);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get()).isEqualTo(testUrlMapping);
        }

        @Test
        @DisplayName("Should return empty optional for non-existent short code")
        void shouldReturnEmptyOptionalForNonExistentShortCode() {
            // Given
            when(urlMappingRepository.findByShortCode(anyString()))
                    .thenReturn(Optional.empty());

            // When
            Optional<UrlMapping> result = urlService.getUrlMapping("nonexistent");

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should return empty optional for null short code")
        void shouldReturnEmptyOptionalForNullShortCode() {
            // When
            Optional<UrlMapping> result = urlService.getUrlMapping(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(urlMappingRepository);
        }
    }

    @Nested
    @DisplayName("shortCodeExists() Tests")
    class ShortCodeExistsTests {

        @Test
        @DisplayName("Should return true for existing short code")
        void shouldReturnTrueForExistingShortCode() {
            // Given
            when(urlMappingRepository.existsByShortCode(testShortCode)).thenReturn(true);

            // When
            boolean result = urlService.shortCodeExists(testShortCode);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for non-existing short code")
        void shouldReturnFalseForNonExistingShortCode() {
            // Given
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);

            // When
            boolean result = urlService.shortCodeExists("nonexistent");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should return false for null short code")
        void shouldReturnFalseForNullShortCode() {
            // When
            boolean result = urlService.shortCodeExists(null);

            // Then
            assertThat(result).isFalse();
            verifyNoInteractions(urlMappingRepository);
        }
    }

    @Nested
    @DisplayName("Utility Methods Tests")
    class UtilityMethodsTests {

        @Test
        @DisplayName("Should return total mappings count")
        void shouldReturnTotalMappingsCount() {
            // Given
            long expectedCount = 42L;
            when(urlMappingRepository.count()).thenReturn(expectedCount);

            // When
            long result = urlService.getTotalMappingsCount();

            // Then
            assertThat(result).isEqualTo(expectedCount);
            verify(urlMappingRepository).count();
        }

        @Test
        @DisplayName("Should delete old mappings")
        void shouldDeleteOldMappings() {
            // Given
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);

            // When
            urlService.deleteOldMappings(cutoffDate);

            // Then
            verify(urlMappingRepository).deleteByCreatedAtBefore(cutoffDate);
        }

        @Test
        @DisplayName("Should not delete when cutoff date is null")
        void shouldNotDeleteWhenCutoffDateIsNull() {
            // When
            urlService.deleteOldMappings(null);

            // Then
            verifyNoInteractions(urlMappingRepository);
        }
    }

    @Nested
    @DisplayName("URL Normalization Tests")
    class UrlNormalizationTests {

        @Test
        @DisplayName("Should preserve existing https protocol")
        void shouldPreserveExistingHttpsProtocol() {
            // Given
            String httpsUrl = "https://example.com/test";
            when(urlMappingRepository.findByLongUrl(httpsUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            urlService.createShortUrl(httpsUrl);

            // Then
            verify(urlMappingRepository).findByLongUrl(httpsUrl);
        }

        @Test
        @DisplayName("Should preserve existing http protocol")
        void shouldPreserveExistingHttpProtocol() {
            // Given
            String httpUrl = "http://example.com/test";
            when(urlMappingRepository.findByLongUrl(httpUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            urlService.createShortUrl(httpUrl);

            // Then
            verify(urlMappingRepository).findByLongUrl(httpUrl);
        }

        @Test
        @DisplayName("Should handle mixed case protocols correctly")
        void shouldHandleMixedCaseProtocolsCorrectly() {
            // Given
            String mixedCaseUrl = "HTTPS://Example.COM/Test";
            when(urlMappingRepository.findByLongUrl(mixedCaseUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            urlService.createShortUrl(mixedCaseUrl);

            // Then
            verify(urlMappingRepository).findByLongUrl(mixedCaseUrl);
        }

        @Test
        @DisplayName("Should preserve case-sensitive URL components")
        void shouldPreserveCaseSensitiveUrlComponents() {
            // Given
            String urlWithCaseSensitiveComponents = "example.com/API/Users?token=aBc123XyZ&userId=ABC123#Profile-Section";
            String expectedNormalizedUrl = "https://example.com/API/Users?token=aBc123XyZ&userId=ABC123#Profile-Section";
            when(urlMappingRepository.findByLongUrl(expectedNormalizedUrl)).thenReturn(Optional.empty());
            when(urlMappingRepository.existsByShortCode(anyString())).thenReturn(false);
            when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(testUrlMapping);

            // When
            urlService.createShortUrl(urlWithCaseSensitiveComponents);

            // Then
            verify(urlMappingRepository).findByLongUrl(expectedNormalizedUrl);
        }
    }
}
