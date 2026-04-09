package com.psyassistant.billing.catalog.dto;

import com.psyassistant.billing.catalog.ServiceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateServiceStatusRequest(
        @NotNull ServiceStatus status
) { }
