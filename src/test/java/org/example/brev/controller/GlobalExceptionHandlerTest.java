package org.example.brev.controller;

import org.example.brev.exception.ShortCodeGenerationException;
import org.example.brev.exception.ShortCodeNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = {GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
@DisplayName("GlobalExceptionHandler Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Should handle ShortCodeNotFoundException with 404 status")
    void shouldHandleShortCodeNotFoundException() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/shortcode-not-found"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Short code not found")))
                .andExpect(jsonPath("$.message", is("Short code 'test123' not found")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle ShortCodeGenerationException with 500 status")
    void shouldHandleShortCodeGenerationException() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/shortcode-generation-error"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Unable to generate short code")))
                .andExpect(jsonPath("$.message", is("Unable to generate a unique short code after maximum attempts. Please try again.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle IllegalArgumentException with 400 status")
    void shouldHandleIllegalArgumentException() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/illegal-argument"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Invalid request")))
                .andExpect(jsonPath("$.message", is("Invalid input provided")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle validation errors with 400 status and field details")
    void shouldHandleValidationErrors() throws Exception {
        // Given
        String invalidJson = "{\"testField\": \"\"}";

        // When & Then
        mockMvc.perform(post("/test/validation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Validation failed")))
                .andExpect(jsonPath("$.message", is("Request validation failed")))
                .andExpect(jsonPath("$.fieldErrors", notNullValue()))
                .andExpect(jsonPath("$.fieldErrors.testField", containsString("must not be blank")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("Should handle generic exception with 500 status")
    void shouldHandleGenericException() throws Exception {
        // When & Then
        mockMvc.perform(get("/test/generic-error"))
                .andDo(print())
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal server error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred. Please try again later.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    /**
     * Test controller to simulate various exception scenarios
     */
    @RestController
    static class TestController {

        @GetMapping("/test/shortcode-not-found")
        public void throwShortCodeNotFoundException() {
            throw new ShortCodeNotFoundException("test123");
        }

        @GetMapping("/test/shortcode-generation-error")
        public void throwShortCodeGenerationException() {
            throw new ShortCodeGenerationException("Unable to generate unique short code");
        }

        @GetMapping("/test/illegal-argument")
        public void throwIllegalArgumentException() {
            throw new IllegalArgumentException("Invalid input provided");
        }

        @GetMapping("/test/generic-error")
        public void throwGenericException() {
            throw new RuntimeException("Database connection failed");
        }

        @PostMapping("/test/validation")
        public void testValidation(@Valid @RequestBody TestRequest request) {
            // This method will trigger validation errors
        }

        static class TestRequest {
            @NotBlank(message = "must not be blank")
            private String testField;

            public String getTestField() {
                return testField;
            }

            public void setTestField(String testField) {
                this.testField = testField;
            }
        }
    }
}
