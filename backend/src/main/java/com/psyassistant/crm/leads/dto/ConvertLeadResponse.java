package com.psyassistant.crm.leads.dto;

import java.util.UUID;

/**
 * Response body returned after a successful lead-to-client conversion.
 *
 * @param clientId UUID of the newly created client record
 * @param leadId   UUID of the lead that was converted
 */
public record ConvertLeadResponse(UUID clientId, UUID leadId) {
}
