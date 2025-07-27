package org.example.brev.controller;

import org.example.brev.exception.ShortCodeNotFoundException;
import org.example.brev.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RedirectController.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("RedirectController Tests")
class RedirectControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService urlService;

    @Test
    @DisplayName("Should redirect to long URL with valid short code")
    void shouldRedirectToLongUrlSuccessfully() throws Exception {
        // Given
        String shortCode = "abc123";
        String longUrl = "https://example.com";
        when(urlService.getLongUrl(shortCode)).thenReturn(longUrl);

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", longUrl));

        verify(urlService, times(1)).getLongUrl(shortCode);
    }

    @Test
    @DisplayName("Should return 404 when short code not found")
    void shouldReturnNotFoundWhenShortCodeNotExists() throws Exception {
        // Given
        String shortCode = "notfound";
        when(urlService.getLongUrl(shortCode))
                .thenThrow(new ShortCodeNotFoundException(shortCode));

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Short code not found")))
                .andExpect(jsonPath("$.message", containsString(shortCode)));

        verify(urlService, times(1)).getLongUrl(shortCode);
    }

    @Test
    @DisplayName("Should return 400 when service throws IllegalArgumentException")
    void shouldReturnBadRequestWhenInvalidShortCode() throws Exception {
        // Given
        String shortCode = "invalid";
        when(urlService.getLongUrl(shortCode))
                .thenThrow(new IllegalArgumentException("Short code cannot be null or empty"));

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid request")))
                .andExpect(jsonPath("$.message", is("Short code cannot be null or empty")));

        verify(urlService, times(1)).getLongUrl(shortCode);
    }

    @Test
    @DisplayName("Should handle various URL formats")
    void shouldHandleVariousUrlFormats() throws Exception {
        // Given
        String shortCode = "test123";
        String longUrl = "https://example.com/path?param=value";
        when(urlService.getLongUrl(shortCode)).thenReturn(longUrl);

        // When & Then
        mockMvc.perform(get("/" + shortCode))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", longUrl));

        verify(urlService, times(1)).getLongUrl(shortCode);
    }
}
