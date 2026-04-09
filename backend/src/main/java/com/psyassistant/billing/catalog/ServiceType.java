package com.psyassistant.billing.catalog;

/**
 * Fixed enumeration of billable service types.
 * Values are persisted as VARCHAR in the {@code service_catalog} table.
 */
public enum ServiceType {
    INDIVIDUAL_SESSION,
    GROUP_SESSION,
    INTAKE_ASSESSMENT,
    FOLLOW_UP
}
