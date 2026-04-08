package com.psyassistant.careplans.dto;

import java.time.Instant;

/** One status transition event in a goal's history. */
public record StatusChangeEvent(
    Instant timestamp,
    String fromStatus,
    String toStatus,
    String actorName
) { }
