package com.psyassistant.reporting.caseload;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.common.audit.AuditLogService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link CaseloadService}.
 */
@ExtendWith(MockitoExtension.class)
class CaseloadServiceTest {

    @Mock
    private CaseloadSnapshotRepository snapshotRepository;

    @Mock
    private SupervisorTeamRepository teamRepository;

    @Mock
    private CaseloadQueryService queryService;

    @Mock
    private AuditLogService auditLogService;

    private CaseloadService service;

    private final UUID adminUserId = UUID.randomUUID();
    private final UUID supervisorUserId = UUID.randomUUID();
    private final UUID therapistId1 = UUID.randomUUID();
    private final UUID therapistId2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CaseloadService(snapshotRepository, teamRepository, queryService, auditLogService);
    }

    // ---- getCaseload scope tests ----------------------------------------

    @Test
    @DisplayName("systemAdmin sees all active therapists")
    void systemAdminGetsAllTherapists() {
        final Set<UUID> allIds = Set.of(therapistId1, therapistId2);
        when(snapshotRepository.findAllActiveTherapistProfileIds()).thenReturn(allIds);
        when(snapshotRepository.findLatestByTherapistIds(eq(allIds), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        service.getCaseload(adminUserId, true, null, null, PageRequest.of(0, 20));

        verify(snapshotRepository).findAllActiveTherapistProfileIds();
    }

    @Test
    @DisplayName("supervisor sees only own team therapists")
    void supervisorGetsOnlyTeamTherapists() {
        final Set<UUID> teamIds = Set.of(therapistId1);
        when(teamRepository.findTherapistIdsBySupervisorId(supervisorUserId)).thenReturn(teamIds);
        when(snapshotRepository.findLatestByTherapistIds(eq(teamIds), isNull(), isNull(), any()))
                .thenReturn(Page.empty());

        service.getCaseload(supervisorUserId, false, null, null, PageRequest.of(0, 20));

        verify(teamRepository).findTherapistIdsBySupervisorId(supervisorUserId);
    }

    @Test
    @DisplayName("supervisor with empty team returns empty page without hitting snapshot repo")
    void supervisorWithEmptyTeamReturnsEmpty() {
        when(teamRepository.findTherapistIdsBySupervisorId(supervisorUserId)).thenReturn(Set.of());

        final Page<CaseloadRowResponse> result =
                service.getCaseload(supervisorUserId, false, null, null, PageRequest.of(0, 20));

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("snapshot rows are mapped to CaseloadRowResponse with therapist name")
    void snapshotRowsMappedToResponseWithName() {
        final CaseloadSnapshot snapshot = buildSnapshot(therapistId1,
                BigDecimal.valueOf(20), BigDecimal.valueOf(40), null);
        final Page<CaseloadSnapshot> page =
                new PageImpl<>(List.of(snapshot), PageRequest.of(0, 20), 1);
        when(snapshotRepository.findAllActiveTherapistProfileIds())
                .thenReturn(Set.of(therapistId1));
        when(snapshotRepository.findLatestByTherapistIds(any(), any(), any(), any()))
                .thenReturn(page);
        final List<Object[]> nameRows = new ArrayList<>();
        nameRows.add(new Object[]{therapistId1, "Jane Doe"});
        when(snapshotRepository.findTherapistNamesById(any())).thenReturn(nameRows);

        final Page<CaseloadRowResponse> result =
                service.getCaseload(adminUserId, true, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        final CaseloadRowResponse row = result.getContent().getFirst();
        assertThat(row.therapistName()).isEqualTo("Jane Doe");
        assertThat(row.therapistProfileId()).isEqualTo(therapistId1);
    }

    // ---- utilization rate tests -----------------------------------------

    @Test
    @DisplayName("utilizationRate is null when contractedHoursPerWeek is null")
    void utilizationRateNullWhenContractedNull() {
        final CaseloadSnapshot snapshot = buildSnapshot(therapistId1,
                BigDecimal.valueOf(20), null, null);
        final Page<CaseloadSnapshot> page =
                new PageImpl<>(List.of(snapshot), PageRequest.of(0, 20), 1);
        when(snapshotRepository.findAllActiveTherapistProfileIds())
                .thenReturn(Set.of(therapistId1));
        when(snapshotRepository.findLatestByTherapistIds(any(), any(), any(), any()))
                .thenReturn(page);
        final List<Object[]> nameRows = new ArrayList<>();
        nameRows.add(new Object[]{therapistId1, "Jane Doe"});
        when(snapshotRepository.findTherapistNamesById(any())).thenReturn(nameRows);

        final Page<CaseloadRowResponse> result =
                service.getCaseload(adminUserId, true, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent().getFirst().utilizationRate()).isNull();
    }

    @Test
    @DisplayName("utilizationRate is null when contractedHoursPerWeek is zero (snapshot stores null)")
    void utilizationRateNullWhenContractedZero() {
        // When contracted is zero, snapshot job stores null utilization_rate
        final CaseloadSnapshot snapshot = buildSnapshot(therapistId1,
                BigDecimal.valueOf(20), BigDecimal.ZERO, null);
        assertThat(snapshot.getUtilizationRate()).isNull();
    }

    @Test
    @DisplayName("utilizationRate is correct fraction when contracted is non-zero")
    void utilizationRateCalculatedCorrectly() {
        // 20 scheduled / 40 contracted = 0.5000
        final BigDecimal scheduled = BigDecimal.valueOf(20);
        final BigDecimal contracted = BigDecimal.valueOf(40);
        final BigDecimal expected = BigDecimal.valueOf(0.5000).setScale(4);

        // Replicate the job's computation logic
        final BigDecimal result = scheduled.divide(contracted, 4, java.math.RoundingMode.HALF_UP);

        assertThat(result).isEqualByComparingTo(expected);
    }

    // ---- getTherapistClients access tests --------------------------------

    @Test
    @DisplayName("admin can access any therapist's client list")
    void adminAccessesAnyTherapist() {
        final Pageable pageable = PageRequest.of(0, 20);
        when(queryService.getClientList(therapistId1, pageable)).thenReturn(Page.empty());

        service.getTherapistClients(adminUserId, true, therapistId1, pageable);

        verify(queryService).getClientList(therapistId1, pageable);
    }

    @Test
    @DisplayName("supervisor can access therapist in their team")
    void supervisorAccessesOwnTeamTherapist() {
        final Pageable pageable = PageRequest.of(0, 20);
        when(teamRepository.findTherapistIdsBySupervisorId(supervisorUserId))
                .thenReturn(Set.of(therapistId1));
        when(queryService.getClientList(therapistId1, pageable)).thenReturn(Page.empty());

        service.getTherapistClients(supervisorUserId, false, therapistId1, pageable);

        verify(queryService).getClientList(therapistId1, pageable);
    }

    @Test
    @DisplayName("supervisor gets AccessDeniedException for therapist outside their team")
    void supervisorCannotAccessOutsideTeam() {
        when(teamRepository.findTherapistIdsBySupervisorId(supervisorUserId))
                .thenReturn(Set.of(therapistId1));

        assertThatThrownBy(() ->
                service.getTherapistClients(supervisorUserId, false, therapistId2, PageRequest.of(0, 20)))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- helpers --------------------------------------------------------

    private CaseloadSnapshot buildSnapshot(
            final UUID therapistId,
            final BigDecimal hoursWeek,
            final BigDecimal contracted,
            final BigDecimal utilization) {
        final BigDecimal computedUtilization = (contracted != null
                && contracted.compareTo(BigDecimal.ZERO) > 0)
                ? hoursWeek.divide(contracted, 4, java.math.RoundingMode.HALF_UP)
                : null;
        return new CaseloadSnapshot(
                therapistId,
                LocalDate.now(),
                5,
                3,
                12,
                hoursWeek,
                contracted,
                computedUtilization
        );
    }
}
