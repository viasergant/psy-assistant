package com.psyassistant.organization;

/**
 * Deployment type of an {@link Organization}.
 *
 * <p>{@code SOLO} organizations have exactly one member (the owner/therapist).
 * {@code CLINIC} organizations support multiple therapists and admin staff.
 */
public enum OrganizationType {
    SOLO,
    CLINIC
}
