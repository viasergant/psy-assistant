package com.psyassistant.crm.leads;

import java.util.UUID;

/**
 * Thrown when an attempt is made to convert a lead that has already been converted
 * to a client record.
 *
 * <p>Mapped to HTTP 409 Conflict by the global exception handler.
 */
public class LeadAlreadyConvertedException extends RuntimeException {

    /** The lead that was requested for conversion. */
    private final UUID leadId;

    /**
     * The UUID of the existing client record, if known.
     * May be null if the back-link on the lead is absent (data inconsistency guard).
     */
    private final UUID existingClientId;

    /**
     * Constructs the exception.
     *
     * @param leadId           the UUID of the already-converted lead
     * @param existingClientId the UUID of the existing client record, or null if unknown
     */
    public LeadAlreadyConvertedException(final UUID leadId, final UUID existingClientId) {
        super("Lead " + leadId + " has already been converted"
                + (existingClientId != null ? " to client " + existingClientId : ""));
        this.leadId = leadId;
        this.existingClientId = existingClientId;
    }

    /**
     * Returns the UUID of the already-converted lead.
     *
     * @return lead UUID
     */
    public UUID getLeadId() {
        return leadId;
    }

    /**
     * Returns the UUID of the existing client record (may be null).
     *
     * @return client UUID or null
     */
    public UUID getExistingClientId() {
        return existingClientId;
    }
}
