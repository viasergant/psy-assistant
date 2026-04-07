package com.psyassistant.sessions.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.psyassistant.sessions.domain.NoteType;
import com.psyassistant.sessions.domain.NoteVisibility;
import com.psyassistant.sessions.domain.SessionNote;
import com.psyassistant.sessions.domain.SessionNoteVersion;
import com.psyassistant.sessions.dto.CreateNoteRequest;
import com.psyassistant.sessions.dto.NoteResponse;
import com.psyassistant.sessions.dto.NoteTemplateFieldResponse;
import com.psyassistant.sessions.dto.NoteTemplateResponse;
import com.psyassistant.sessions.dto.NoteVersionResponse;
import com.psyassistant.sessions.dto.UpdateNoteRequest;
import com.psyassistant.sessions.repository.NoteTemplateRepository;
import com.psyassistant.sessions.repository.SessionNoteRepository;
import com.psyassistant.sessions.repository.SessionNoteVersionRepository;
import com.psyassistant.users.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for creating, reading, and updating clinical session notes.
 *
 * <p><strong>RBAC rules:</strong>
 * <ul>
 *   <li>CREATE / UPDATE: {@code WRITE_SESSION_NOTE} (THERAPIST only)</li>
 *   <li>READ: {@code READ_OWN_SESSION_NOTES} (THERAPIST) or
 *       {@code READ_ALL_SESSION_NOTES} (SUPERVISOR / SYSTEM_ADMINISTRATOR)</li>
 * </ul>
 * Post-authorization visibility filtering:
 * <ul>
 *   <li>THERAPIST: sees only their own notes (all visibility levels)</li>
 *   <li>SUPERVISOR / SYS_ADMIN: sees only {@code SUPERVISOR_VISIBLE} notes (any author)</li>
 * </ul>
 *
 * <p>Content security: free-form HTML is sanitized with OWASP Java HTML Sanitizer before
 * storage.  Structured field values are stored as a JSON string (Map→JSON).
 *
 * <p>Encryption: handled transparently by {@link com.psyassistant.common.config.EncryptedStringConverter}.
 */
@Service
public class SessionNoteService {

    private static final Logger LOG = LoggerFactory.getLogger(SessionNoteService.class);
    private static final String SHA_256 = "SHA-256";

    /** OWASP sanitizer policy: allow clinical prose markup only. */
    private static final PolicyFactory HTML_POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "strong", "em", "u", "ul", "ol", "li", "br",
                           "h1", "h2", "h3", "blockquote")
            .toFactory();

    private final SessionNoteRepository noteRepository;
    private final SessionNoteVersionRepository versionRepository;
    private final NoteTemplateRepository templateRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a new {@code SessionNoteService}.
     */
    public SessionNoteService(
            final SessionNoteRepository noteRepository,
            final SessionNoteVersionRepository versionRepository,
            final NoteTemplateRepository templateRepository,
            final UserRepository userRepository,
            final ObjectMapper objectMapper) {
        this.noteRepository = noteRepository;
        this.versionRepository = versionRepository;
        this.templateRepository = templateRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // Create
    // =========================================================================

    /**
     * Creates a new session note authored by {@code principal}.
     *
     * @param sessionRecordId target session record UUID
     * @param request         note content and settings
     * @param principal       authenticated user's principal name
     * @return response DTO for the newly created note
     */
    @Transactional
    @PreAuthorize("hasAuthority('WRITE_SESSION_NOTE')")
    public NoteResponse createNote(
            final UUID sessionRecordId,
            final CreateNoteRequest request,
            final String principal) {

        validateNoteContent(request.noteType(), request.content(), request.structuredFields());

        final SessionNote note = new SessionNote(
                sessionRecordId,
                principal,
                request.noteType(),
                request.visibility() != null ? request.visibility() : NoteVisibility.PRIVATE);

        applyContent(note, request.noteType(), request.content(), request.structuredFields());

        final SessionNote saved = noteRepository.save(note);
        LOG.info("Created session note: id={}, session={}, author={}, type={}",
                saved.getId(), sessionRecordId, principal, request.noteType());

        return toResponse(saved, false);
    }

    // =========================================================================
    // Update
    // =========================================================================

    /**
     * Updates an existing note.  Only the original author may update their own note.
     * If the content hash matches the current version, the update is skipped (idempotent).
     *
     * @param sessionRecordId owning session record UUID (validated for consistency)
     * @param noteId          note UUID
     * @param request         updated content
     * @param principal       authenticated user's principal name
     * @return updated note response
     */
    @Transactional
    @PreAuthorize("hasAuthority('WRITE_SESSION_NOTE')")
    public NoteResponse updateNote(
            final UUID sessionRecordId,
            final UUID noteId,
            final UpdateNoteRequest request,
            final String principal) {

        final SessionNote note = loadNote(noteId);

        if (!note.getSessionRecordId().equals(sessionRecordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found in this session");
        }
        if (!note.getAuthorId().equals(principal)) {
            throw new AccessDeniedException("You can only edit your own notes");
        }

        // Compute new hash
        final String newHash = computeHash(note.getNoteType(), request.content(),
                request.structuredFields());

        // Idempotency: skip version creation if content is identical
        if (newHash.equals(note.getContentHash())) {
            LOG.debug("Skipping note update — content unchanged: noteId={}", noteId);
            return toResponse(note, versionRepository.countByNoteId(noteId) > 0);
        }

        // Capture current version before mutation
        final int nextVersion = versionRepository.countByNoteId(noteId) + 1;
        final SessionNoteVersion version = new SessionNoteVersion(
                noteId,
                nextVersion,
                note.getContent(),
                note.getStructuredFields(),
                note.getContentHash(),
                note.getAuthorId(),
                note.getVisibility());
        versionRepository.save(version);

        // Apply new content
        if (request.visibility() != null) {
            note.setVisibility(request.visibility());
        }
        applyContent(note, note.getNoteType(), request.content(), request.structuredFields());

        final SessionNote saved = noteRepository.save(note);
        LOG.info("Updated session note: id={}, version={}", noteId, nextVersion);

        return toResponse(saved, true);
    }

    // =========================================================================
    // Read — notes list
    // =========================================================================

    /**
     * Lists all notes for a session record that the caller is allowed to see.
     *
     * <p>Visibility filtering applied after the authority check:
     * <ul>
     *   <li>{@code READ_OWN_SESSION_NOTES}: returns notes where {@code author_id == principal}</li>
     *   <li>{@code READ_ALL_SESSION_NOTES}: returns notes where
     *       {@code visibility == SUPERVISOR_VISIBLE}</li>
     * </ul>
     * Roles with neither permission receive HTTP 403 from the controller; this method
     * is never reached for them.
     */
    @Transactional(readOnly = true)
    public List<NoteResponse> listNotes(
            final UUID sessionRecordId,
            final String principal,
            final boolean hasReadOwn,
            final boolean hasReadAll) {

        if (!hasReadOwn && !hasReadAll) {
            throw new AccessDeniedException("You do not have permission to view clinical notes");
        }

        final List<SessionNote> notes;

        if (hasReadOwn && !hasReadAll) {
            // THERAPIST: own notes only, all visibility levels
            notes = noteRepository.findBySessionRecordIdAndAuthorIdOrderByCreatedAtAsc(
                    sessionRecordId, principal);
        } else {
            // SUPERVISOR / SYS_ADMIN: SUPERVISOR_VISIBLE notes from any author
            notes = noteRepository.findBySessionRecordIdOrderByCreatedAtAsc(sessionRecordId)
                    .stream()
                    .filter(n -> n.getVisibility() == NoteVisibility.SUPERVISOR_VISIBLE)
                    .toList();
        }

        return notes.stream()
                .map(n -> toResponse(n, versionRepository.countByNoteId(n.getId()) > 0))
                .toList();
    }

    // =========================================================================
    // Read — version history
    // =========================================================================

    /**
     * Returns the chronological version history for a note.
     *
     * @param sessionRecordId owning session record UUID
     * @param noteId          note UUID
     * @param principal       caller's principal name
     * @param hasReadOwn      caller has READ_OWN_SESSION_NOTES authority
     * @param hasReadAll      caller has READ_ALL_SESSION_NOTES authority
     * @return ordered list of version snapshots (oldest first)
     */
    @Transactional(readOnly = true)
    public List<NoteVersionResponse> getVersionHistory(
            final UUID sessionRecordId,
            final UUID noteId,
            final String principal,
            final boolean hasReadOwn,
            final boolean hasReadAll) {

        if (!hasReadOwn && !hasReadAll) {
            throw new AccessDeniedException("You do not have permission to view clinical notes");
        }

        final SessionNote note = loadNote(noteId);

        if (!note.getSessionRecordId().equals(sessionRecordId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found in this session");
        }

        // Apply same visibility filtering as list operation
        if (hasReadOwn && !hasReadAll && !note.getAuthorId().equals(principal)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
        }
        if (hasReadAll && !hasReadOwn
                && note.getVisibility() != NoteVisibility.SUPERVISOR_VISIBLE) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found");
        }

        return versionRepository.findByNoteIdOrderByVersionNumberAsc(noteId)
                .stream()
                .map(this::toVersionResponse)
                .toList();
    }

    // =========================================================================
    // Templates
    // =========================================================================

    /**
     * Lists all available note templates.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('WRITE_SESSION_NOTE')")
    public List<NoteTemplateResponse> listTemplates() {
        return templateRepository.findAll().stream()
                .map(t -> new NoteTemplateResponse(
                        t.getId(), t.getName(), t.getDescription(),
                        parseTemplateFields(t.getTemplateFields()), t.getCreatedAt()))
                .toList();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private SessionNote loadNote(final UUID noteId) {
        return noteRepository.findById(noteId)
                .orElseThrow(() -> new EntityNotFoundException("Note not found: " + noteId));
    }

    /**
     * Sanitizes HTML, serializes structured fields, computes hash, and sets them on the note.
     */
    private void applyContent(
            final SessionNote note,
            final NoteType type,
            final String content,
            final Map<String, String> structuredFields) {

        if (type == NoteType.FREE_FORM) {
            final String sanitized = HTML_POLICY.sanitize(content != null ? content : "");
            note.setContent(sanitized);
            note.setStructuredFields(null);
            note.setContentHash(sha256(sanitized));
        } else {
            final String json = serializeFields(structuredFields);
            note.setStructuredFields(json);
            note.setContent(null);
            note.setContentHash(sha256(json));
        }
    }

    private String computeHash(
            final NoteType type, final String content, final Map<String, String> fields) {
        if (type == NoteType.FREE_FORM) {
            return sha256(HTML_POLICY.sanitize(content != null ? content : ""));
        }
        return sha256(serializeFields(fields));
    }

    private void validateNoteContent(
            final NoteType type,
            final String content,
            final Map<String, String> structuredFields) {
        if (type == NoteType.FREE_FORM && (content == null || content.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "content is required for FREE_FORM notes");
        }
        if (type == NoteType.STRUCTURED
                && (structuredFields == null || structuredFields.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "structuredFields is required for STRUCTURED notes");
        }
    }

    private List<NoteTemplateFieldResponse> parseTemplateFields(final String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json,
                    new TypeReference<List<NoteTemplateFieldResponse>>() { });
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to parse template fields JSON, returning empty list", e);
            return List.of();
        }
    }

    private String serializeFields(final Map<String, String> fields) {
        try {
            return objectMapper.writeValueAsString(fields != null ? fields : Map.of());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize structured fields", e);
        }
    }

    private Map<String, String> deserializeFields(final String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() { });
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to deserialize structured fields, returning null", e);
            return null;
        }
    }

    private String sha256(final String input) {
        try {
            final MessageDigest digest = MessageDigest.getInstance(SHA_256);
            final byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String resolveAuthorName(final String authorId) {
        if (authorId == null) {
            return "Unknown";
        }
        return userRepository.findByEmail(authorId)
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getEmail())
                .orElse(authorId);
    }

    private NoteResponse toResponse(final SessionNote note, final boolean hasVersionHistory) {
        return new NoteResponse(
                note.getId(),
                note.getSessionRecordId(),
                note.getNoteType(),
                note.getVisibility(),
                note.getContent(),
                note.getNoteType() == NoteType.STRUCTURED
                        ? deserializeFields(note.getStructuredFields()) : null,
                resolveAuthorName(note.getAuthorId()),
                note.getCreatedAt(),
                note.getUpdatedAt(),
                hasVersionHistory);
    }

    private NoteVersionResponse toVersionResponse(final SessionNoteVersion v) {
        return new NoteVersionResponse(
                v.getId(),
                v.getVersionNumber(),
                v.getContent(),
                v.getStructuredFields() != null ? deserializeFields(v.getStructuredFields()) : null,
                resolveAuthorName(v.getAuthorId()),
                v.getVisibility(),
                v.getCreatedAt());
    }
}
