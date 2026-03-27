package com.psyassistant.crm.leads.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * A single contact method entry used in create/update lead requests.
 *
 * @param type      contact type: EMAIL or PHONE
 * @param value     the actual email address or phone number
 * @param isPrimary whether this is the preferred contact method
 */
public record ContactMethodRequest(
        @NotNull(message = "Contact method type must not be null")
        @Pattern(regexp = "EMAIL|PHONE", message = "Type must be EMAIL or PHONE")
        String type,

        @NotBlank(message = "Contact method value must not be blank")
        @Size(max = 255, message = "Contact method value must not exceed 255 characters")
        String value,

        boolean isPrimary
) {
}
