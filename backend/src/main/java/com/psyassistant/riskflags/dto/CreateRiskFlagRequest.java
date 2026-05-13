package com.psyassistant.riskflags.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request payload for creating a new risk flag on a client profile.
 *
 * @param flagTypeId    UUID of the risk flag type; must be active
 * @param clinicalNote  optional clinical note; visible only to users with READ_RISK_FLAG_NOTES
 * @param reviewDate    date by which the flag must be reviewed; today or in the future
 */
public record CreateRiskFlagRequest(
        @NotNull UUID flagTypeId,
        String clinicalNote,
        @NotNull @FutureOrPresent LocalDate reviewDate
) {
}
