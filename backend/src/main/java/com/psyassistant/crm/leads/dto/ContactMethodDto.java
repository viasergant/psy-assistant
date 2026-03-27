package com.psyassistant.crm.leads.dto;

import com.psyassistant.crm.leads.LeadContactMethod;
import java.util.UUID;

/**
 * Read-only projection of a single contact method.
 *
 * @param id        contact method UUID
 * @param type      EMAIL or PHONE
 * @param value     the actual email address or phone number
 * @param isPrimary whether this is the primary contact channel
 */
public record ContactMethodDto(UUID id, String type, String value, boolean isPrimary) {

    /**
     * Maps a {@link LeadContactMethod} entity to a {@link ContactMethodDto}.
     *
     * @param cm the contact method entity
     * @return the DTO
     */
    public static ContactMethodDto from(final LeadContactMethod cm) {
        return new ContactMethodDto(cm.getId(), cm.getType(), cm.getValue(), cm.isPrimary());
    }
}
