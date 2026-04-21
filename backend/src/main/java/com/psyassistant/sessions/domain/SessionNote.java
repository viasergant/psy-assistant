package com.psyassistant.sessions.domain;

import com.psyassistant.common.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Clinical note attached to a {@link SessionRecord}.
 *
 * <p>A therapist may write multiple notes per session (e.g. one free-form and one
 * structured).  Each note maintains its own version history via {@link SessionNoteVersion}.
 *
 * <p>The {@code content} and {@code structuredFields} columns are encrypted at rest via
 * {@link EncryptedStringConverter} (AES-256-GCM).
 */
@Entity
@Table(name = "session_note")
@EntityListeners(AuditingEntityListener.class)
public class SessionNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to the parent session record. Never changes after creation. */
    @Column(name = "session_record_id", nullable = false, updatable = false)
    private UUID sessionRecordId;

    /** Principal name of the note author (matches JWT sub claim). */
    @Column(name = "author_id", nullable = false, updatable = false)
    private String authorId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "note_type", nullable = false, updatable = false, columnDefinition = "note_type")
    private NoteType noteType;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, columnDefinition = "note_visibility")
    private NoteVisibility visibility = NoteVisibility.PRIVATE;

    /** Encrypted rich-text HTML content (FREE_FORM notes). */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** Encrypted JSON string representing the structured field map (STRUCTURED notes). */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "structured_fields", columnDefinition = "TEXT")
    private String structuredFields;

    /** SHA-256 of the plaintext content used for idempotent save detection. */
    @Column(name = "content_hash", length = 64)
    private String contentHash;

    /**
     * Scope of the note within a session.
     * SESSION = shared group-level note; CLIENT = per-client private note.
     * Defaults to SESSION so existing individual session notes are unaffected.
     */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "note_scope", nullable = false, columnDefinition = "note_scope")
    private NoteScope noteScope = NoteScope.SESSION;

    /**
     * For CLIENT-scoped notes: the target client within the group session.
     * Null for SESSION-scoped notes (including all notes on INDIVIDUAL sessions).
     */
    @Column(name = "target_client_id")
    private UUID targetClientId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected SessionNote() {
    }

    /**
     * Factory constructor for creating new SESSION-scoped notes (INDIVIDUAL or shared GROUP).
     */
    public SessionNote(
            final UUID sessionRecordId,
            final String authorId,
            final NoteType noteType,
            final NoteVisibility visibility) {
        this.sessionRecordId = sessionRecordId;
        this.authorId = authorId;
        this.noteType = noteType;
        this.visibility = visibility;
        this.noteScope = NoteScope.SESSION;
        this.targetClientId = null;
    }

    /**
     * Factory constructor for creating CLIENT-scoped per-client notes within a group session.
     */
    public SessionNote(
            final UUID sessionRecordId,
            final String authorId,
            final NoteType noteType,
            final NoteVisibility visibility,
            final UUID targetClientId) {
        this.sessionRecordId = sessionRecordId;
        this.authorId = authorId;
        this.noteType = noteType;
        this.visibility = visibility;
        this.noteScope = NoteScope.CLIENT;
        this.targetClientId = targetClientId;
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getSessionRecordId() {
        return sessionRecordId;
    }

    public String getAuthorId() {
        return authorId;
    }

    public NoteType getNoteType() {
        return noteType;
    }

    public NoteVisibility getVisibility() {
        return visibility;
    }

    public void setVisibility(final NoteVisibility visibility) {
        this.visibility = visibility;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }

    public String getStructuredFields() {
        return structuredFields;
    }

    public void setStructuredFields(final String structuredFields) {
        this.structuredFields = structuredFields;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(final String contentHash) {
        this.contentHash = contentHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public NoteScope getNoteScope() {
        return noteScope;
    }

    public void setNoteScope(final NoteScope noteScope) {
        this.noteScope = noteScope;
    }

    public UUID getTargetClientId() {
        return targetClientId;
    }

    public void setTargetClientId(final UUID targetClientId) {
        this.targetClientId = targetClientId;
    }
}
