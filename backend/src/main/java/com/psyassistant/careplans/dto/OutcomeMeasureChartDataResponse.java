package com.psyassistant.careplans.dto;

import java.util.List;

/** Chart-ready series for a single outcome measure across time. */
public record OutcomeMeasureChartDataResponse(
    String measureCode,
    String displayName,
    Integer alertThreshold,
    List<OutcomeMeasureChartDataPoint> series
) { }
