package com.psyassistant.common.security;

/**
 * Fine-grained permissions that can be held by a {@link com.psyassistant.users.UserRole}.
 *
 * <p>Permissions are emitted as plain authority strings in the JWT {@code roles} claim
 * (without the {@code ROLE_} prefix).  They are used with Spring Security's
 * {@code hasAuthority()} check in {@code @PreAuthorize} annotations at the service layer.
 *
 * <p>The authoritative mapping of roles to permissions lives in
 * {@link RolePermissions}.
 */
public enum Permission {

    // ---- Client / lead management ----------------------------------------
    /** Create, update, and archive client and lead records. */
    MANAGE_CLIENTS,

    /** Read any client profile regardless of therapist assignment. */
    READ_CLIENTS_ALL,

    /** Read client profiles assigned to the requesting therapist. */
    READ_ASSIGNED_CLIENTS,

    /** Create, update, and cancel appointments. */
    MANAGE_APPOINTMENTS,

    /** Create, update, and close leads. */
    MANAGE_LEADS,

    /** Read lead records. */
    READ_LEADS,

    // ---- Sessions --------------------------------------------------------
    /** Read session records for clients assigned to the requesting therapist only. */
    READ_OWN_SESSIONS,

    /** Read session records for all clients regardless of assignment. */
    READ_ALL_SESSIONS,

    // ---- Session notes ---------------------------------------------------
    /** Create and update session notes for sessions the therapist owns. */
    WRITE_SESSION_NOTE,

    /** Read session notes for sessions assigned to the requesting therapist only. */
    READ_OWN_SESSION_NOTES,

    /** Read session notes for all sessions regardless of assignment. */
    READ_ALL_SESSION_NOTES,

    // ---- Care plans ------------------------------------------------------
    /** Read care plan documents. */
    READ_CARE_PLANS,

    /** Create and update care plan documents including goals, interventions, milestones. */
    MANAGE_CARE_PLANS,

    // ---- Finance ---------------------------------------------------------
    /** Create, update, and void invoices. */
    MANAGE_INVOICES,

    /** Create draft invoices from sessions, packages, or manual entries. */
    CREATE_INVOICES,

    /** Issue (lock and publish) draft invoices. */
    ISSUE_INVOICES,

    /** Cancel draft or issued invoices. */
    CANCEL_INVOICES,

    /** Read invoice list and detail views. */
    READ_INVOICES,

    /** Record and reconcile payments and refunds. */
    MANAGE_PAYMENTS,

    /** Access financial summary and detailed reports. */
    READ_FINANCIAL_REPORTS,

    // ---- Reporting / workload --------------------------------------------
    /** View team workload dashboards. */
    READ_TEAM_WORKLOAD,

    /** Access operational and clinical reports. */
    READ_REPORTS,

    // ---- Administration --------------------------------------------------
    /** Create, update, deactivate, and change roles of internal user accounts. */
    MANAGE_USERS,

    /** Access the audit log. */
    VIEW_AUDIT_LOG,

    /** Read and write system-level configuration settings. */
    MANAGE_SYSTEM_CONFIG,

    // ---- Service catalog -------------------------------------------------
    /** Create, update, and deactivate service catalog entries and manage prices. */
    MANAGE_SERVICE_CATALOG,

    /** Read service catalog entries and price history. */
    READ_SERVICE_CATALOG,

    // ---- Pricing rules ---------------------------------------------------
    /** Create and update per-therapist session-type pricing rules. */
    MANAGE_PRICING_RULES,

    /** Read per-therapist session-type pricing rules. */
    READ_PRICING_RULES
}
