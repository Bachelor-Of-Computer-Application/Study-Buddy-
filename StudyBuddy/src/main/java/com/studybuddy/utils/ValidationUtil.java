package com.studybuddy.utils;

import com.studybuddy.models.RegisterInput;

/**
 * Utility class for validating user input fields.
 */
public class ValidationUtil {

    /**
     * Validates registration inputs.
     *
     * @param input Data containing registration inputs
     * @return true if valid
     */
    public static boolean isValidRegistration(RegisterInput input) {
        // Name validation
        if (input.getName() == null || input.getName().trim().isEmpty()) {
            return false;
        }

        // Email validation
        if (input.getEmail() == null || !input.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            return false;
        }

        // Password validation (min 6 characters)
        if (input.getPassword() == null || input.getPassword().length() < 6) {
            return false;
        }

        // Confirm password match
        if (input.getConfirmPassword() == null || !input.getPassword().equals(input.getConfirmPassword())) {
            return false;
        }

        return true;
    }
}
