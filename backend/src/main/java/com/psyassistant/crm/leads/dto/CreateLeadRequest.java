package com.psyassistant.crm.leads.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request body for creating a new lead.
 *
 * @param fullName        the lead's full display name (required)
 * @param contactMethods  one or more contact methods (required, at least one)
 * @param source          optional acquisition source (e.g. "referral")
 * @param ownerId         optional UUID of the owning staff member
 * @param notes           optional free-text notes
 */
public record CreateLeadRequest(
        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotEmpty(message = "At least one contact method is required")
        @Valid
        List<ContactMethodRequest> contactMethods,

        @Size(max = 100, message = "Source must not exceed 100 characters")
        String source,

        UUID ownerId,

        String notes
) {
}
