package com.psyassistant.auth.service;

import com.psyassistant.common.config.SecurityProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Validates a plaintext password against the configured complexity policy.
 *
 * <p>Returns a list of violation codes; an empty list means the password is valid.
 * Violation codes: {@code MIN_LENGTH}, {@code REQUIRES_UPPERCASE},
 * {@code REQUIRES_DIGIT}, {@code REQUIRES_SPECIAL_CHAR}.
 */
@Component
public class PasswordComplexityValidator {

    private static final String SPECIAL_CHARS = "!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~";

    private final SecurityProperties securityProperties;

    /**
     * Constructs the validator with the security policy configuration.
     *
     * @param securityProperties security policy settings
     */
    public PasswordComplexityValidator(final SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    /**
     * Validates the password and returns a list of violation codes.
     * An empty list indicates the password satisfies all requirements.
     *
     * @param password plaintext password to validate
     * @return list of violation codes; empty if valid
     */
    public List<String> validate(final String password) {
        SecurityProperties.PasswordProperties policy = securityProperties.password();
        List<String> violations = new ArrayList<>();

        if (password == null || password.length() < policy.minLength()) {
            violations.add("MIN_LENGTH");
        }

        if (policy.requireUppercase() && (password == null || !containsUppercase(password))) {
            violations.add("REQUIRES_UPPERCASE");
        }

        if (policy.requireDigit() && (password == null || !containsDigit(password))) {
            violations.add("REQUIRES_DIGIT");
        }

        if (policy.requireSpecialChar() && (password == null || !containsSpecialChar(password))) {
            violations.add("REQUIRES_SPECIAL_CHAR");
        }

        return violations;
    }

    private boolean containsUppercase(final String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(final String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsSpecialChar(final String password) {
        for (char c : password.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && !Character.isWhitespace(c)) {
                return true;
            }
        }
        return false;
    }
}
