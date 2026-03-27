package com.psyassistant.crm.leads.dto;

import com.psyassistant.crm.leads.Lead;
import com.psyassistant.crm.leads.LeadStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Compact lead representation used in paginated list responses.
 *
 * <p>Shows only the primary contact method value to keep the response lightweight.
 *
 * @param id              lead UUID
 * @param fullName        full display name
 * @param primaryContact  value of the primary contact method (email or phone)
 * @param source          optional acquisition source
 * @param status          current lifecycle status
 * @param ownerName       optional owner display name
 * @param lastContactDate system-managed timestamp of most recent contact
 * @param createdAt       record creation timestamp
 */
public record LeadSummaryDto(
        UUID id,
        String fullName,
        String primaryContact,
        String source,
        LeadStatus status,
        String ownerName,
        Instant lastContactDate,
        Instant createdAt
) {

    /**
     * Builds a {@link LeadSummaryDto} from a {@link Lead} entity and an optional owner name.
     *
     * @param lead      the lead entity
     * @param ownerName optional display name of the owning staff member
     * @return the populated summary DTO
     */
    public static LeadSummaryDto from(final Lead lead, final String ownerName) {
        String primaryContact = lead.getContactMethods().stream()
                .filter(cm -> cm.isPrimary())
                .map(cm -> cm.getValue())
                .findFirst()
                .orElseGet(() -> lead.getContactMethods().isEmpty()
                        ? null
                        : lead.getContactMethods().get(0).getValue());

        return new LeadSummaryDto(
                lead.getId(),
                lead.getFullName(),
                primaryContact,
                lead.getSource(),
                lead.getStatus(),
                ownerName,
                lead.getLastContactDate(),
                lead.getCreatedAt()
        );
    }
}
