package com.psyassistant.careplans.dto;

import java.time.LocalDate;

/** A single data point for an outcome measure chart. */
public record OutcomeMeasureChartDataPoint(
    LocalDate date,
    int score,
    boolean thresholdBreached
) { }
