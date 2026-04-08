package com.psyassistant.careplans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to add an immutable progress note to a goal. */
public record CreateProgressNoteRequest(
    @NotBlank @Size(max = 2000) String noteText
) { }
