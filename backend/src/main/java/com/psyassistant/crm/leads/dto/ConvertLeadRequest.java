package com.psyassistant.crm.leads.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * Request body for converting a qualified lead into a client record.
 *
 * @param fullName       the client's full display name (required)
 * @param contactMethods one or more contact methods (required, at least one)
 * @param notes          optional free-text notes to include in the client record
 * @param ownerId        optional UUID of the staff member to assign as owner;
 *                       defaults to the lead's owner, then to the acting principal
 */
public record ConvertLeadRequest(
        @NotBlank(message = "Full name must not be blank")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        @NotEmpty(message = "At least one contact method is required")
        @Valid
        List<ContactMethodRequest> contactMethods,

        String notes,

        UUID ownerId
) {
}
