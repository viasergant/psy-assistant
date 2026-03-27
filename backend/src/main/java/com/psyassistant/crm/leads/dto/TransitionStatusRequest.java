package com.psyassistant.crm.leads.dto;

import com.psyassistant.crm.leads.LeadStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for transitioning a lead to a new status.
 *
 * @param status the desired target status
 */
public record TransitionStatusRequest(
        @NotNull(message = "Status must not be null")
        LeadStatus status
) {
}
