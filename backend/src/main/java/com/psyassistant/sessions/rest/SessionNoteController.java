package com.psyassistant.sessions.rest;

import com.psyassistant.sessions.dto.CreateNoteRequest;
import com.psyassistant.sessions.dto.NoteResponse;
import com.psyassistant.sessions.dto.NoteTemplateResponse;
import com.psyassistant.sessions.dto.NoteVersionResponse;
import com.psyassistant.sessions.dto.UpdateNoteRequest;
import com.psyassistant.sessions.service.SessionNoteService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for session note management.
 *
 * <p>Exposes endpoints under {@code /api/sessions/{sessionId}/notes} for creating,
 * reading, and updating clinical notes, plus {@code /api/notes/templates} for template discovery.
 *
 * <p>Access control is enforced at two levels:
 * <ol>
 *   <li>Controller-level: 403 returned immediately for roles lacking any note permission.</li>
 *   <li>Service-level: visibility-based filtering (own notes vs SUPERVISOR_VISIBLE).</li>
 * </ol>
 */
@RestController
public class SessionNoteController {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNoteController.class);

    private static final String PERMISSION_WRITE = "WRITE_SESSION_NOTE";
    private static final String PERMISSION_READ_OWN = "READ_OWN_SESSION_NOTES";
    private static final String PERMISSION_READ_ALL = "READ_ALL_SESSION_NOTES";

    private final SessionNoteService noteService;

    /**
     * Constructs the controller with the required service dependency.
     *
     * @param noteService the session note service
     */
    public SessionNoteController(final SessionNoteService noteService) {
        this.noteService = noteService;
    }

    // =========================================================================
    // Note endpoints
    // =========================================================================

    /**
     * Lists all notes the caller may see for the given session.
     *
     * <p>GET /api/sessions/{sessionId}/notes
     *
     * <p>Returns 403 if the caller has neither read permission.
     */
    @GetMapping("/api/sessions/{sessionId}/notes")
    public ResponseEntity<List<NoteResponse>> listNotes(
            @PathVariable final UUID sessionId,
            final Authentication auth) {

        final boolean hasReadOwn = hasAuthority(auth, PERMISSION_READ_OWN);
        final boolean hasReadAll = hasAuthority(auth, PERMISSION_READ_ALL);

        if (!hasReadOwn && !hasReadAll) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have permission to view clinical notes");
        }

        final List<NoteResponse> notes = noteService.listNotes(
                sessionId, auth.getName(), hasReadOwn, hasReadAll);

        return ResponseEntity.ok(notes);
    }

    /**
     * Creates a new note for the given session.
     *
     * <p>POST /api/sessions/{sessionId}/notes
     */
    @PostMapping("/api/sessions/{sessionId}/notes")
    public ResponseEntity<NoteResponse> createNote(
            @PathVariable final UUID sessionId,
            @Valid @RequestBody final CreateNoteRequest request,
            final Principal principal) {

        LOG.debug("POST /api/sessions/{}/notes — type={}, visibility={}",
                sessionId, request.noteType(), request.visibility());

        final NoteResponse response = noteService.createNote(sessionId, request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Updates an existing note (author-only).
     *
     * <p>PUT /api/sessions/{sessionId}/notes/{noteId}
     */
    @PutMapping("/api/sessions/{sessionId}/notes/{noteId}")
    public ResponseEntity<NoteResponse> updateNote(
            @PathVariable final UUID sessionId,
            @PathVariable final UUID noteId,
            @RequestBody final UpdateNoteRequest request,
            final Principal principal) {

        LOG.debug("PUT /api/sessions/{}/notes/{} — author={}", sessionId, noteId, principal.getName());

        final NoteResponse response = noteService.updateNote(
                sessionId, noteId, request, principal.getName());
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the version history for a note.
     *
     * <p>GET /api/sessions/{sessionId}/notes/{noteId}/history
     */
    @GetMapping("/api/sessions/{sessionId}/notes/{noteId}/history")
    public ResponseEntity<List<NoteVersionResponse>> getNoteHistory(
            @PathVariable final UUID sessionId,
            @PathVariable final UUID noteId,
            final Authentication auth) {

        final boolean hasReadOwn = hasAuthority(auth, PERMISSION_READ_OWN);
        final boolean hasReadAll = hasAuthority(auth, PERMISSION_READ_ALL);

        if (!hasReadOwn && !hasReadAll) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "You do not have permission to view clinical notes");
        }

        final List<NoteVersionResponse> history = noteService.getVersionHistory(
                sessionId, noteId, auth.getName(), hasReadOwn, hasReadAll);

        return ResponseEntity.ok(history);
    }

    // =========================================================================
    // Template endpoints
    // =========================================================================

    /**
     * Lists available note templates.
     *
     * <p>GET /api/notes/templates
     */
    @GetMapping("/api/notes/templates")
    public ResponseEntity<List<NoteTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(noteService.listTemplates());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private boolean hasAuthority(final Authentication auth, final String authority) {
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }
}
