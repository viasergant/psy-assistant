package com.psyassistant.sessions.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.sessions.domain.NoteType;
import com.psyassistant.sessions.domain.NoteVisibility;
import com.psyassistant.sessions.domain.SessionNote;
import com.psyassistant.sessions.domain.SessionNoteVersion;
import com.psyassistant.sessions.dto.CreateNoteRequest;
import com.psyassistant.sessions.dto.NoteResponse;
import com.psyassistant.sessions.dto.UpdateNoteRequest;
import com.psyassistant.sessions.repository.NoteTemplateRepository;
import com.psyassistant.sessions.repository.SessionNoteRepository;
import com.psyassistant.sessions.repository.SessionNoteVersionRepository;
import com.psyassistant.users.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link SessionNoteService}.
 *
 * <p>Covers all RBAC / visibility combinations and idempotency behaviour.
 */
@ExtendWith(MockitoExtension.class)
class SessionNoteServiceTest {

    @Mock private SessionNoteRepository noteRepository;
    @Mock private SessionNoteVersionRepository versionRepository;
    @Mock private NoteTemplateRepository templateRepository;
    @Mock private UserRepository userRepository;

    private SessionNoteService service;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String THERAPIST = "therapist@example.com";
    private static final String OTHER_THERAPIST = "other@example.com";

    @BeforeEach
    void setUp() {
        service = new SessionNoteService(
                noteRepository, versionRepository, templateRepository,
                userRepository, new ObjectMapper());
        lenient().when(userRepository.findByEmail(any())).thenReturn(Optional.empty());
    }

    // =========================================================================
    // Create
    // =========================================================================

    @Nested
    @DisplayName("createNote")
    class CreateNote {

        @Test
        @DisplayName("creates FREE_FORM note and persists it")
        void createFreeFormNote() {
            final CreateNoteRequest req = new CreateNoteRequest(
                    NoteType.FREE_FORM, NoteVisibility.PRIVATE, "<p>Hello</p>", null, null, null);

            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final NoteResponse resp = service.createNote(SESSION_ID, req, THERAPIST);

            assertThat(resp.noteType()).isEqualTo(NoteType.FREE_FORM);
            assertThat(resp.visibility()).isEqualTo(NoteVisibility.PRIVATE);
            assertThat(resp.content()).isEqualTo("<p>Hello</p>");
            assertThat(resp.structuredFields()).isNull();
            verify(noteRepository).save(any(SessionNote.class));
        }

        @Test
        @DisplayName("creates STRUCTURED note and persists it")
        void createStructuredNote() {
            final Map<String, String> fields = Map.of(
                    "presentingProblem", "Anxiety",
                    "interventionsUsed", "CBT");
            final CreateNoteRequest req = new CreateNoteRequest(
                    NoteType.STRUCTURED, NoteVisibility.SUPERVISOR_VISIBLE, null, fields, null, null);

            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final NoteResponse resp = service.createNote(SESSION_ID, req, THERAPIST);

            assertThat(resp.noteType()).isEqualTo(NoteType.STRUCTURED);
            assertThat(resp.visibility()).isEqualTo(NoteVisibility.SUPERVISOR_VISIBLE);
            assertThat(resp.structuredFields()).containsEntry("presentingProblem", "Anxiety");
        }

        @Test
        @DisplayName("rejects FREE_FORM with blank content")
        void rejectsBlankFreeForm() {
            final CreateNoteRequest req = new CreateNoteRequest(
                    NoteType.FREE_FORM, NoteVisibility.PRIVATE, "", null, null, null);

            assertThatThrownBy(() -> service.createNote(SESSION_ID, req, THERAPIST))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("rejects STRUCTURED with no fields")
        void rejectsEmptyStructuredFields() {
            final CreateNoteRequest req = new CreateNoteRequest(
                    NoteType.STRUCTURED, NoteVisibility.PRIVATE, null, Map.of(), null, null);

            assertThatThrownBy(() -> service.createNote(SESSION_ID, req, THERAPIST))
                    .isInstanceOf(ResponseStatusException.class);
        }

        @Test
        @DisplayName("strips disallowed HTML tags (XSS prevention)")
        void sanitizesHtml() {
            final CreateNoteRequest req = new CreateNoteRequest(
                    NoteType.FREE_FORM, NoteVisibility.PRIVATE,
                    "<p>Safe</p><script>alert(1)</script>", null, null, null);

            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            final NoteResponse resp = service.createNote(SESSION_ID, req, THERAPIST);

            assertThat(resp.content()).doesNotContain("<script>");
            assertThat(resp.content()).contains("Safe");
        }
    }

    // =========================================================================
    // Update
    // =========================================================================

    @Nested
    @DisplayName("updateNote")
    class UpdateNote {

        private SessionNote existingNote;
        private UUID noteId;

        @BeforeEach
        void setUpNote() {
            noteId = UUID.randomUUID();
            existingNote = new SessionNote(SESSION_ID, THERAPIST, NoteType.FREE_FORM, NoteVisibility.PRIVATE);
            existingNote.setContent("<p>Original</p>");
            existingNote.setContentHash("some-hash");
        }

        @Test
        @DisplayName("captures version when content changes")
        void capturesVersionOnChange() {
            final UpdateNoteRequest req = new UpdateNoteRequest(
                    null, "<p>Updated</p>", null);

            when(noteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));
            when(versionRepository.countByNoteId(noteId)).thenReturn(0);
            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateNote(SESSION_ID, noteId, req, THERAPIST);

            verify(versionRepository).save(any(SessionNoteVersion.class));
        }

        @Test
        @DisplayName("skips version entry when content is identical (idempotency)")
        void skipsVersionOnIdenticalContent() {
            // Use same content — the hash will match
            final String sameContent = "<p>Original</p>";
            final UpdateNoteRequest req = new UpdateNoteRequest(null, sameContent, null);

            // Pre-compute the expected hash for "<p>Original</p>" (after sanitization)
            existingNote.setContent(sameContent);
            // Force the stored hash to match what the service will compute
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));
            when(versionRepository.countByNoteId(noteId)).thenReturn(0);
            // Trigger save of note once with same content to get the hash populated
            when(noteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Create the note first to get the hash
            final CreateNoteRequest createReq = new CreateNoteRequest(
                    NoteType.FREE_FORM, NoteVisibility.PRIVATE, sameContent, null, null, null);
            service.createNote(SESSION_ID, createReq, THERAPIST);

            // Now simulate existing note already having that hash
            final ArgumentCaptor<SessionNote> captor = ArgumentCaptor.forClass(SessionNote.class);
            verify(noteRepository).save(captor.capture());
            final String computedHash = captor.getValue().getContentHash();
            existingNote.setContentHash(computedHash);

            // Reset mock
            org.mockito.Mockito.reset(versionRepository, noteRepository);
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));
            when(versionRepository.countByNoteId(noteId)).thenReturn(0);

            service.updateNote(SESSION_ID, noteId, req, THERAPIST);

            verify(versionRepository, never()).save(any());
            verify(noteRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws 403 when non-author tries to update")
        void rejectsNonAuthorUpdate() {
            when(noteRepository.findById(noteId)).thenReturn(Optional.of(existingNote));

            assertThatThrownBy(() ->
                    service.updateNote(SESSION_ID, noteId,
                            new UpdateNoteRequest(null, "<p>Hack</p>", null), OTHER_THERAPIST))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // =========================================================================
    // Read / Visibility filtering
    // =========================================================================

    @Nested
    @DisplayName("listNotes — RBAC filtering")
    class ListNotesRbac {

        private SessionNote privateNote;
        private SessionNote supervisorNote;

        @BeforeEach
        void setUpNotes() {
            privateNote = new SessionNote(SESSION_ID, THERAPIST, NoteType.FREE_FORM, NoteVisibility.PRIVATE);
            privateNote.setContent("<p>Private</p>");
            privateNote.setContentHash("hash1");

            supervisorNote = new SessionNote(SESSION_ID, OTHER_THERAPIST, NoteType.FREE_FORM,
                    NoteVisibility.SUPERVISOR_VISIBLE);
            supervisorNote.setContent("<p>Supervisor</p>");
            supervisorNote.setContentHash("hash2");
        }

        @Test
        @DisplayName("THERAPIST sees only own notes")
        void therapistSeesOnlyOwnNotes() {
            when(noteRepository.findBySessionRecordIdAndAuthorIdOrderByCreatedAtAsc(
                    SESSION_ID, THERAPIST))
                    .thenReturn(List.of(privateNote));
            when(versionRepository.countByNoteId(any())).thenReturn(0);

            final List<NoteResponse> result = service.listNotes(SESSION_ID, THERAPIST, true, false);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).visibility()).isEqualTo(NoteVisibility.PRIVATE);
        }

        @Test
        @DisplayName("SUPERVISOR sees only SUPERVISOR_VISIBLE notes from any author")
        void supervisorSeesOnlySupervisorVisible() {
            when(noteRepository.findBySessionRecordIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(privateNote, supervisorNote));
            when(versionRepository.countByNoteId(any())).thenReturn(0);

            final List<NoteResponse> result = service.listNotes(SESSION_ID, "supervisor@example.com",
                    false, true);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).visibility()).isEqualTo(NoteVisibility.SUPERVISOR_VISIBLE);
        }

        @Test
        @DisplayName("role with neither permission receives AccessDeniedException")
        void noPermissionThrows() {
            assertThatThrownBy(() -> service.listNotes(SESSION_ID, "finance@example.com", false, false))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("PRIVATE notes are invisible to SUPERVISOR")
        void privateNoteHiddenFromSupervisor() {
            lenient().when(noteRepository.findBySessionRecordIdOrderByCreatedAtAsc(SESSION_ID))
                    .thenReturn(List.of(privateNote));
            lenient().when(versionRepository.countByNoteId(any())).thenReturn(0);

            final List<NoteResponse> result = service.listNotes(SESSION_ID, "supervisor@example.com",
                    false, true);

            assertThat(result).isEmpty();
        }
    }
}
