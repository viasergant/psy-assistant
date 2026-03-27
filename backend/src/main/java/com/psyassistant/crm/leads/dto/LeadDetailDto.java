package com.psyassistant.crm.leads.dto;

import com.psyassistant.crm.leads.Lead;
import com.psyassistant.crm.leads.LeadStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full detail view of a single lead returned by create, update, and get-by-id endpoints.
 *
 * @param id                lead UUID
 * @param fullName          full display name
 * @param contactMethods    all contact methods
 * @param source            optional acquisition source
 * @param status            current lifecycle status
 * @param ownerId           optional owner user UUID
 * @param ownerName         optional owner display name (resolved by service)
 * @param notes             optional free-text notes
 * @param lastContactDate   system-managed timestamp of most recent contact
 * @param createdAt         record creation timestamp
 * @param updatedAt         record last-modified timestamp
 * @param createdBy         email/name of the principal that created the record
 * @param convertedClientId UUID of the client this lead was converted to, or null
 */
public record LeadDetailDto(
        UUID id,
        String fullName,
        List<ContactMethodDto> contactMethods,
        String source,
        LeadStatus status,
        UUID ownerId,
        String ownerName,
        String notes,
        Instant lastContactDate,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        UUID convertedClientId
) {

    /**
     * Builds a {@link LeadDetailDto} from a {@link Lead} entity and an optional owner name.
     *
     * @param lead      the lead entity
     * @param ownerName optional display name of the owning staff member
     * @return the populated DTO
     */
    public static LeadDetailDto from(final Lead lead, final String ownerName) {
        List<ContactMethodDto> methods = lead.getContactMethods().stream()
                .map(ContactMethodDto::from)
                .toList();

        return new LeadDetailDto(
                lead.getId(),
                lead.getFullName(),
                methods,
                lead.getSource(),
                lead.getStatus(),
                lead.getOwnerId(),
                ownerName,
                lead.getNotes(),
                lead.getLastContactDate(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                lead.getCreatedBy(),
                lead.getConvertedClientId()
        );
    }
}
