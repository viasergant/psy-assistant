package com.psyassistant.careplans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.psyassistant.careplans.domain.CarePlan;
import com.psyassistant.careplans.domain.CarePlanGoal;
import com.psyassistant.careplans.domain.GoalProgressNote;
import com.psyassistant.careplans.dto.GoalProgressNoteResponse;
import com.psyassistant.careplans.exception.CarePlanNotActiveException;
import com.psyassistant.careplans.repository.CarePlanAuditRepository;
import com.psyassistant.careplans.repository.CarePlanRepository;
import com.psyassistant.careplans.repository.GoalProgressNoteRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link GoalProgressService}.
 */
@ExtendWith(MockitoExtension.class)
class GoalProgressServiceTest {

    @Mock
    private CarePlanRepository carePlanRepository;

    @Mock
    private GoalProgressNoteRepository noteRepository;

    @Mock
    private CarePlanAuditRepository auditRepository;

    @Mock
    private CarePlanAuditService auditService;

    @Mock
    private UserRepository userRepository;

    private GoalProgressService service;

    private UUID clientId;
    private UUID therapistId;
    private UUID otherTherapistId;
    private UUID planId;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        clientId = UUID.randomUUID();
        therapistId = UUID.randomUUID();
        otherTherapistId = UUID.randomUUID();
        planId = UUID.randomUUID();
        goalId = UUID.randomUUID();

        service = new GoalProgressService(
                carePlanRepository,
                noteRepository,
                auditRepository,
                auditService,
                userRepository);
    }

    // -------------------------------------------------------------------------
    // addProgressNote
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("addProgressNote - saves note and writes audit for plan owner")
    void addProgressNoteSavesAndAuditsForOwner() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(noteRepository.save(any(GoalProgressNote.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        final GoalProgressNoteResponse response = service.addProgressNote(
                planId, goalId, "Initial improvement noted.", therapistId, "Dr. Smith");

        assertThat(response.noteText()).isEqualTo("Initial improvement noted.");
        assertThat(response.authorName()).isEqualTo("Dr. Smith");
        verify(noteRepository).save(any(GoalProgressNote.class));
        verify(auditService).recordGoalProgressNoteAdded(planId, goalId, therapistId, "Dr. Smith");
    }

    @Test
    @DisplayName("addProgressNote - throws CarePlanNotActiveException when plan is closed")
    void addProgressNoteThrowsWhenPlanNotActive() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        plan.close(therapistId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() ->
                service.addProgressNote(planId, goalId, "Note", therapistId, "Dr. Smith"))
                .isInstanceOf(CarePlanNotActiveException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addProgressNote - throws AccessDeniedException for non-owner")
    void addProgressNoteThrowsForNonOwner() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() ->
                service.addProgressNote(planId, goalId, "Note", otherTherapistId, "Other"))
                .isInstanceOf(AccessDeniedException.class);

        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("addProgressNote - throws EntityNotFoundException when plan does not exist")
    void addProgressNoteThrowsWhenPlanNotFound() {
        when(carePlanRepository.findById(planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.addProgressNote(planId, goalId, "Note", therapistId, "Dr. Smith"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // -------------------------------------------------------------------------
    // getProgressNotes
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProgressNotes - returns all notes when hasReadAll is true (supervisor)")
    void getProgressNotesReturnsAllForSupervisor() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final GoalProgressNote note1 = progressNote(goalId, planId, therapistId, "Note A");
        final GoalProgressNote note2 = progressNote(goalId, planId, otherTherapistId, "Note B");
        when(noteRepository.findByGoalIdOrderByCreatedAtDesc(goalId))
                .thenReturn(List.of(note1, note2));

        final List<GoalProgressNoteResponse> result =
                service.getProgressNotes(planId, goalId, otherTherapistId, true);

        assertThat(result).hasSize(2);
        verify(noteRepository).findByGoalIdOrderByCreatedAtDesc(goalId);
        verify(noteRepository, never()).findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("getProgressNotes - returns only own notes when hasReadAll is false")
    void getProgressNotesReturnsOwnNotesForTherapist() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        final GoalProgressNote ownNote = progressNote(goalId, planId, therapistId, "My note");
        when(noteRepository.findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(goalId, therapistId))
                .thenReturn(List.of(ownNote));

        final List<GoalProgressNoteResponse> result =
                service.getProgressNotes(planId, goalId, therapistId, false);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).noteText()).isEqualTo("My note");
        verify(noteRepository).findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(goalId, therapistId);
        verify(noteRepository, never()).findByGoalIdOrderByCreatedAtDesc(any());
    }

    @Test
    @DisplayName("getProgressNotes - throws AccessDeniedException when non-owner therapist accesses plan")
    void getProgressNotesThrowsForNonOwnerWithoutReadAll() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));

        assertThatThrownBy(() ->
                service.getProgressNotes(planId, goalId, otherTherapistId, false))
                .isInstanceOf(AccessDeniedException.class);
    }

    // -------------------------------------------------------------------------
    // getProgressHistory
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getProgressHistory - returns combined status events and notes for plan owner")
    void getProgressHistoryReturnsCombinedDataForOwner() {
        final CarePlan plan = activePlanWithGoal(planId, clientId, therapistId, goalId);
        when(carePlanRepository.findById(planId)).thenReturn(Optional.of(plan));
        when(auditRepository.findByCarePlanIdOrderByActionTimestampDesc(planId))
                .thenReturn(List.of());
        when(noteRepository.findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(goalId, therapistId))
                .thenReturn(List.of());

        final var response = service.getProgressHistory(planId, goalId, therapistId, false);

        assertThat(response).isNotNull();
        assertThat(response.goalId()).isEqualTo(goalId);
        assertThat(response.statusHistory()).isEmpty();
        assertThat(response.progressNotes()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private CarePlan activePlanWithGoal(
            final UUID pId, final UUID cId, final UUID tId, final UUID gId) {
        final CarePlan plan = new CarePlan(cId, tId, "Test Plan", null);
        ReflectionTestUtils.setField(plan, "id", pId);

        final CarePlanGoal goal = new CarePlanGoal(plan, "Test Goal", (short) 1, null);
        ReflectionTestUtils.setField(goal, "id", gId);
        plan.getGoals().add(goal);

        return plan;
    }

    private GoalProgressNote progressNote(
            final UUID gId, final UUID pId, final UUID authorId, final String text) {
        final GoalProgressNote note = new GoalProgressNote(gId, pId, text, authorId, "Test Author");
        ReflectionTestUtils.setField(note, "id", UUID.randomUUID());
        return note;
    }
}
