package com.psyassistant.sessions.domain;

import com.psyassistant.common.config.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;

/**
 * Immutable snapshot of a {@link SessionNote} at the point it was superseded by an edit.
 *
 * <p>Rows in this table are <em>never</em> deleted — the 3-year data retention policy is
 * enforced at the operational / infrastructure layer, not in application code.
 *
 * <p>The entity intentionally does not extend {@link com.psyassistant.common.audit.BaseEntity}
 * because the version timestamp is set once at creation and must never be touched again.
 */
@Entity
@Table(name = "session_note_version")
public class SessionNoteVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** FK to the parent note. */
    @Column(name = "note_id", nullable = false, updatable = false)
    private UUID noteId;

    /** 1-based sequential version counter within a note lifecycle. */
    @Column(name = "version_number", nullable = false, updatable = false)
    private int versionNumber;

    /** Encrypted rich-text HTML snapshot (FREE_FORM notes). */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "content", updatable = false, columnDefinition = "TEXT")
    private String content;

    /** Encrypted JSON map snapshot (STRUCTURED notes). */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "structured_fields", updatable = false, columnDefinition = "TEXT")
    private String structuredFields;

    /** SHA-256 of the plaintext at this version. */
    @Column(name = "content_hash", length = 64, updatable = false)
    private String contentHash;

    /** Principal name of the author who wrote this version. */
    @Column(name = "author_id", nullable = false, updatable = false)
    private String authorId;

    /** Visibility setting at the time this version was saved. */
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "visibility", nullable = false, updatable = false, columnDefinition = "note_visibility")
    private NoteVisibility visibility;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    protected SessionNoteVersion() { }

    /**
     * Full constructor used by the service layer when capturing a version snapshot.
     */
    public SessionNoteVersion(
            final UUID noteId,
            final int versionNumber,
            final String content,
            final String structuredFields,
            final String contentHash,
            final String authorId,
            final NoteVisibility visibility) {
        this.noteId = noteId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.structuredFields = structuredFields;
        this.contentHash = contentHash;
        this.authorId = authorId;
        this.visibility = visibility;
        this.createdAt = Instant.now();
    }

    // -------------------------------------------------------------------------
    // Accessors (read-only; no setters — entity is immutable after creation)
    // -------------------------------------------------------------------------

    public UUID getId() {
        return id;
    }

    public UUID getNoteId() {
        return noteId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getContent() {
        return content;
    }

    public String getStructuredFields() {
        return structuredFields;
    }

    public String getContentHash() {
        return contentHash;
    }

    public String getAuthorId() {
        return authorId;
    }

    public NoteVisibility getVisibility() {
        return visibility;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
