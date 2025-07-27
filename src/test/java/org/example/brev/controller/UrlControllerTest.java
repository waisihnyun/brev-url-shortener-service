package org.example.brev.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.brev.dto.CreateUrlRequest;
import org.example.brev.entity.UrlMapping;
import org.example.brev.exception.ShortCodeGenerationException;
import org.example.brev.service.UrlService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("UrlController Tests")
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    private CreateUrlRequest validRequest;
    private UrlMapping mockUrlMapping;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUrlRequest("https://example.com");
        mockUrlMapping = new UrlMapping("https://example.com", "abc123");
        mockUrlMapping.setId(1L);
        mockUrlMapping.setCreatedAt(LocalDateTime.of(2025, 7, 24, 22, 7, 16));
    }

    @Nested
    @DisplayName("POST /api/v1/urls - Create Short URL Tests")
    class CreateShortUrlTests {

        @Test
        @DisplayName("Should create short URL successfully with valid request")
        void shouldCreateShortUrlSuccessfully() throws Exception {
            // Given
            when(urlService.createShortUrl(anyString())).thenReturn(mockUrlMapping);

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(1)))
                    .andExpect(jsonPath("$.longUrl", is("https://example.com")))
                    .andExpect(jsonPath("$.shortCode", is("abc123")))
                    .andExpect(jsonPath("$.shortUrl", is("http://localhost/abc123")))
                    .andExpect(jsonPath("$.createdAt", is("2025-07-24T22:07:16")));

            verify(urlService, times(1)).createShortUrl("https://example.com");
        }

        @Test
        @DisplayName("Should return 400 when longUrl is null")
        void shouldReturnBadRequestWhenLongUrlIsNull() throws Exception {
            // Given
            CreateUrlRequest invalidRequest = new CreateUrlRequest(null);

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Validation failed")))
                    .andExpect(jsonPath("$.fieldErrors.longUrl", containsString("cannot be blank")));

            verify(urlService, never()).createShortUrl(anyString());
        }

        @Test
        @DisplayName("Should return 400 when longUrl is blank")
        void shouldReturnBadRequestWhenLongUrlIsBlank() throws Exception {
            // Given
            CreateUrlRequest invalidRequest = new CreateUrlRequest("   ");

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors.longUrl", containsString("cannot be blank")));

            verify(urlService, never()).createShortUrl(anyString());
        }

        @Test
        @DisplayName("Should return 400 when longUrl is not a valid URL")
        void shouldReturnBadRequestWhenLongUrlIsInvalid() throws Exception {
            // Given
            CreateUrlRequest invalidRequest = new CreateUrlRequest("not-a-valid-url");

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.fieldErrors.longUrl", containsString("must be a valid URL")));

            verify(urlService, never()).createShortUrl(anyString());
        }

        @Test
        @DisplayName("Should return 400 when longUrl exceeds maximum length")
        void shouldReturnBadRequestWhenLongUrlTooLong() throws Exception {
            // Given - Create a long URL that exceeds 2048 characters
            StringBuilder longUrlBuilder = new StringBuilder("https://example.com/");
            for (int i = 0; i < 2000; i++) {
                longUrlBuilder.append("a");
            }
            String longUrl = longUrlBuilder.toString();
            CreateUrlRequest invalidRequest = new CreateUrlRequest(longUrl);

            // Mock the service to throw IllegalArgumentException if called (though validation should prevent this)
            when(urlService.createShortUrl(anyString()))
                    .thenThrow(new IllegalArgumentException("Long URL cannot exceed 2048 characters"));

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", anyOf(is("Validation failed"), is("Invalid request"))))
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        // Check that the response contains the expected error message about URL length
                        if (!response.contains("cannot exceed 2048 characters")) {
                            throw new AssertionError("Response should contain message about URL length limit: " + response);
                        }
                    });

            // Note: The service may or may not be called depending on whether DTO validation catches it first
        }

        @Test
        @DisplayName("Should return 400 when service throws IllegalArgumentException")
        void shouldReturnBadRequestWhenServiceThrowsIllegalArgument() throws Exception {
            // Given
            when(urlService.createShortUrl(anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid URL format"));

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(400)))
                    .andExpect(jsonPath("$.error", is("Invalid request")))
                    .andExpect(jsonPath("$.message", is("Invalid URL format")));

            verify(urlService, times(1)).createShortUrl("https://example.com");
        }

        @Test
        @DisplayName("Should return 500 when service throws ShortCodeGenerationException")
        void shouldReturnInternalServerErrorWhenCannotGenerateShortCode() throws Exception {
            // Given
            when(urlService.createShortUrl(anyString()))
                    .thenThrow(new ShortCodeGenerationException("Unable to generate unique short code"));

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(500)))
                    .andExpect(jsonPath("$.error", is("Unable to generate short code")))
                    .andExpect(jsonPath("$.message", containsString("Unable to generate a unique short code")));

            verify(urlService, times(1)).createShortUrl("https://example.com");
        }

        @Test
        @DisplayName("Should return 500 when service throws unexpected exception")
        void shouldReturnInternalServerErrorWhenUnexpectedException() throws Exception {
            // Given
            when(urlService.createShortUrl(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest)))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", is(500)))
                    .andExpect(jsonPath("$.error", is("Internal server error")))
                    .andExpect(jsonPath("$.message", containsString("An unexpected error occurred")));

            verify(urlService, times(1)).createShortUrl("https://example.com");
        }

        @Test
        @DisplayName("Should return 400 when request body is missing")
        void shouldReturnBadRequestWhenRequestBodyMissing() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(urlService, never()).createShortUrl(anyString());
        }

        @Test
        @DisplayName("Should return 400 when request content type is not JSON")
        void shouldReturnBadRequestWhenContentTypeNotJson() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/v1/urls")
                    .contentType(MediaType.TEXT_PLAIN)
                    .content("https://example.com"))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(urlService, never()).createShortUrl(anyString());
        }

        @Test
        @DisplayName("Should handle various valid URL formats")
        void shouldHandleVariousValidUrlFormats() throws Exception {
            // Test different valid URL formats
            String[] validUrls = {
                "https://www.example.com",
                "http://example.com",
                "https://example.com/path/to/resource",
                "https://example.com:8080/path?param=value",
                "https://subdomain.example.com",
                "https://example.com/path#fragment"
            };

            for (String url : validUrls) {
                // Given
                CreateUrlRequest request = new CreateUrlRequest(url);
                UrlMapping mapping = new UrlMapping(url, "test123");
                mapping.setId(1L);
                mapping.setCreatedAt(LocalDateTime.now());

                when(urlService.createShortUrl(url)).thenReturn(mapping);

                // When & Then
                mockMvc.perform(post("/api/v1/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.longUrl", is(url)));

                verify(urlService, times(1)).createShortUrl(url);
                reset(urlService);
            }
        }
    }
}
