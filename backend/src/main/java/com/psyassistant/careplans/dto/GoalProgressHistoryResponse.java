package com.psyassistant.careplans.dto;

import java.util.List;
import java.util.UUID;

/** Combined goal progress history: status changes + progress notes. */
public record GoalProgressHistoryResponse(
    UUID goalId,
    List<StatusChangeEvent> statusHistory,
    List<GoalProgressNoteResponse> progressNotes
) { }
