package org.example.brev.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Custom validator for long URLs that ensures consistent validation order
 */
public class ValidLongUrlValidator implements ConstraintValidator<ValidLongUrl, String> {

    private static final int MAX_URL_LENGTH = 2048;

    @Override
    public void initialize(ValidLongUrl constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Disable default constraint violation
        context.disableDefaultConstraintViolation();

        // 1. First check if the value is null or blank (after trimming)
        if (value == null || value.trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Long URL cannot be blank")
                    .addConstraintViolation();
            return false;
        }

        // 2. Then check the length
        if (value.length() > MAX_URL_LENGTH) {
            context.buildConstraintViolationWithTemplate("Long URL cannot exceed " + MAX_URL_LENGTH + " characters")
                    .addConstraintViolation();
            return false;
        }

        // 3. Finally check if it's a valid URL format
        if (!isValidUrl(value)) {
            context.buildConstraintViolationWithTemplate("Long URL must be a valid URL")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }

    private boolean isValidUrl(String url) {
        try {
            new URL(url.trim());
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
