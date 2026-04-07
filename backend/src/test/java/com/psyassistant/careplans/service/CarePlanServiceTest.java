package com.psyassistant.careplans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.careplans.config.CarePlanProperties;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanStatus;
import com.psyassistant.careplans.dto.CarePlanDetailResponse;
import com.psyassistant.careplans.dto.CarePlanSummaryResponse;
import com.psyassistant.careplans.dto.CreateCarePlanRequest;
import com.psyassistant.careplans.dto.CreateGoalRequest;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.exception.MaxActivePlansExceededException;
import com.psyassistant.careplans.repository.CarePlanAuditRepository;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/**
 * Unit tests for {@link CarePlanService}.
 */
@ExtendWith(MockitoExtension.class)
class CarePlanServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private CarePlanAuditRepository carePlanAuditRepository;

    @Mock
    private CarePlanAuditService auditService;

    @Mock
    private UserRepository userRepository;

    private CarePlanProperties properties;

    private CarePlanService service;

    private UUID clientId;
    private UUID therapistId;
    private UUID otherTherapistId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        otherTherapistId = UUID.randomUUID();
        actorId = therapistId;

        properties = new CarePlanProperties(3, List.of("CBT", "DBT", "EMDR"));

        service = new CarePlanService(
                carePlanRepository,
                carePlanAuditRepository,
                auditService,
                properties,
                userRepository
        );
    }

    // -------------------------------------------------------------------------
    // listByClient
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("listByClient - returns all plans when actor hasReadAll")
    void listByClientReturnsAllWhenHasReadAll() {
        final CarePlan plan1 = activePlan(clientId, therapistId);
        final CarePlan plan2 = activePlan(clientId, otherTherapistId);

        when(carePlanRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(plan1, plan2));

        final List<CarePlanSummaryResponse> result =
                service.listByClient(clientId, null, actorId, true);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("listByClient - filters to own plans when actor does not have hasReadAll")
    void listByClientFiltersToOwnPlans() {
        final CarePlan ownPlan = activePlan(clientId, therapistId);
        final CarePlan otherPlan = activePlan(clientId, otherTherapistId);

        when(carePlanRepository.findByClientIdOrderByCreatedAtDesc(clientId))
                .thenReturn(List.of(ownPlan, otherPlan));

        final List<CarePlanSummaryResponse> result =
                service.listByClient(clientId, null, actorId, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).therapistId()).isEqualTo(therapistId);
    }

    @Test
    @DisplayName("listByClient - uses status filter when provided")
    void listByClientUsesStatusFilter() {
        when(carePlanRepository.findByClientIdAndStatusOrderByCreatedAtDesc(clientId, CarePlanStatus.ACTIVE))
                .thenReturn(List.of(activePlan(clientId, therapistId)));

        final List<CarePlanSummaryResponse> result =
                service.listByClient(clientId, CarePlanStatus.ACTIVE, actorId, true);

        assertThat(result).hasSize(1);
        verify(carePlanRepository, never()).findByClientIdOrderByCreatedAtDesc(any());
    }

    // -------------------------------------------------------------------------
    // create
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create - throws MaxActivePlansExceededException when limit reached")
    void createThrowsWhenMaxActivePlansReached() {
        when(carePlanRepository.countActiveByClientId(clientId))
                .thenReturn((long) properties.maxActivePerClient());

        final CreateCarePlanRequest request = new CreateCarePlanRequest(
                "Plan title", null, List.of(goalRequest()));

        assertThatThrownBy(() -> service.create(clientId, request, actorId, "Test Actor"))
                .isInstanceOf(MaxActivePlansExceededException.class);

        verify(carePlanRepository, never()).save(any());
    }

    @Test
    @DisplayName("create - saves plan and records audit when under limit")
    void createSavesPlanAndAuditsWhenUnderLimit() {
        when(carePlanRepository.countActiveByClientId(clientId)).thenReturn(0L);

        final CarePlan savedPlan = activePlan(clientId, actorId);
        when(carePlanRepository.save(any(CarePlan.class))).thenReturn(savedPlan);

        final CreateCarePlanRequest request = new CreateCarePlanRequest(
                "Plan title", null, List.of(goalRequest()));

        final CarePlanDetailResponse response = service.create(clientId, request, actorId, "Actor Name");

        assertThat(response).isNotNull();
        verify(carePlanRepository).save(any(CarePlan.class));
        verify(auditService).recordPlanCreated(any(CarePlan.class), any(UUID.class), any(String.class));
    }

    // -------------------------------------------------------------------------
    // getDetail
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getDetail - throws EntityNotFoundException when plan not found")
    void getDetailThrowsWhenNotFound() {
        final UUID planId = UUID.randomUUID();
        when(carePlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(planId, actorId, true))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("getDetail - throws AccessDeniedException when actor is not plan owner and hasReadAll is false")
    void getDetailThrowsAccessDeniedForNonOwner() {
        final UUID planId = UUID.randomUUID();
        final CarePlan plan = activePlan(clientId, otherTherapistId);

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.getDetail(planId, actorId, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("getDetail - returns detail for owner")
    void getDetailReturnsDetailForOwner() {
        final UUID planId = UUID.randomUUID();
        final CarePlan plan = activePlan(clientId, therapistId);

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final CarePlanDetailResponse response = service.getDetail(planId, actorId, false);

        assertThat(response).isNotNull();
        assertThat(response.therapistId()).isEqualTo(therapistId);
    }

    // -------------------------------------------------------------------------
    // close / archive
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("close - transitions plan to CLOSED")
    void closePlanTransitionsToClosed() {
        final UUID planId = UUID.randomUUID();
        final CarePlan plan = activePlan(clientId, therapistId);

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(carePlanRepository.save(plan)).thenReturn(plan);

        service.close(planId, actorId, "Actor Name");

        assertThat(plan.getStatus()).isEqualTo(CarePlanStatus.CLOSED);
        verify(auditService).recordPlanClosed(any(), any(), any());
    }

    @Test
    @DisplayName("close - throws CarePlanNotActiveException when plan is already closed")
    void closeThrowsWhenPlanNotActive() {
        final UUID planId = UUID.randomUUID();
        final CarePlan plan = activePlan(clientId, therapistId);
        plan.close(actorId); // already closed

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() -> service.close(planId, actorId, "Actor Name"))
                .isInstanceOf(CarePlanNotActiveException.class);
    }

    @Test
    @DisplayName("archive - transitions CLOSED plan to ARCHIVED")
    void archivePlanTransitionsToArchived() {
        final UUID planId = UUID.randomUUID();
        final CarePlan plan = activePlan(clientId, therapistId);
        plan.close(actorId); // must be closed first -> CLOSED

        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(carePlanRepository.save(plan)).thenReturn(plan);

        service.archive(planId, actorId, "Actor Name");

        assertThat(plan.getStatus()).isEqualTo(CarePlanStatus.ARCHIVED);
        verify(auditService).recordPlanArchived(any(), any(), any());
    }

    // -------------------------------------------------------------------------
    // getInterventionTypes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getInterventionTypes - returns configured types")
    void getInterventionTypesReturnsConfigured() {
        final List<String> types = service.getInterventionTypes();
        assertThat(types).containsExactlyInAnyOrder("CBT", "DBT", "EMDR");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CarePlan activePlan(final UUID cId, final UUID tId) {
        return new CarePlan(cId, tId, "Test Plan", null);
    }

    private CreateGoalRequest goalRequest() {
        return new CreateGoalRequest("Sample goal description", (short) 1, null,
                List.of(), List.of());
    }
}
