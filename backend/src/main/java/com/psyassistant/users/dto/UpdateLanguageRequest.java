package com.psyassistant.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating the current user's language preference.
 */
@Schema(description = "Language preference update request")
public record UpdateLanguageRequest(

        @Schema(description = "Language code (en, uk)",
                example = "uk",
                required = true)
        @NotBlank(message = "Language code is required")
        @Pattern(regexp = "^(en|uk)$", message = "Language must be 'en' or 'uk'")
        String language
) {
}
