package com.psyassistant.riskflags.dto;

import com.psyassistant.riskflags.domain.ClientRiskFlagStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for a single risk flag.
 *
 * <p>{@code clinicalNote} is included in this record but the service layer sets it to {@code null}
 * for callers who lack the {@code READ_RISK_FLAG_NOTES} authority.
 *
 * @param id                 flag UUID
 * @param clientId           client the flag belongs to
 * @param flagTypeId         UUID of the flag type
 * @param flagTypeName       display name of the flag type at response time
 * @param status             current lifecycle status (ACTIVE or RESOLVED)
 * @param clinicalNote       optional clinical note; null when caller lacks READ_RISK_FLAG_NOTES
 * @param reviewDate         date by which the flag must be reviewed
 * @param createdByUserId    user who raised the flag
 * @param createdAt          server-managed creation timestamp
 * @param resolvedByUserId   user who resolved the flag; null if still active
 * @param resolvedAt         resolution timestamp; null if still active
 * @param resolutionNote     explanation provided at resolution; null if still active
 */
public record RiskFlagResponse(
        UUID id,
        UUID clientId,
        UUID flagTypeId,
        String flagTypeName,
        ClientRiskFlagStatus status,
        String clinicalNote,
        LocalDate reviewDate,
        UUID createdByUserId,
        Instant createdAt,
        UUID resolvedByUserId,
        Instant resolvedAt,
        String resolutionNote
) {
}
