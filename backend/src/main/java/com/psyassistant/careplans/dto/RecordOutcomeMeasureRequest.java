package com.psyassistant.careplans.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

/** Request to record a new outcome measure entry on a care plan. */
public record RecordOutcomeMeasureRequest(
    @NotNull UUID measureDefinitionId,
    @NotNull Integer score,
    @NotNull @PastOrPresent LocalDate assessmentDate,
    @Size(max = 1000) String notes
) { }
