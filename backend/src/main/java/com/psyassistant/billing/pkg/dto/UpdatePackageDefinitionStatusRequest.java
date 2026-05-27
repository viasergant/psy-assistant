package com.psyassistant.billing.pkg.dto;

import com.psyassistant.billing.pkg.PackageDefinitionStatus;
import jakarta.validation.constraints.NotNull;

/** Request to change the status of a prepaid package definition (e.g. ACTIVE → ARCHIVED). */
public record UpdatePackageDefinitionStatusRequest(
    @NotNull PackageDefinitionStatus status
) { }
