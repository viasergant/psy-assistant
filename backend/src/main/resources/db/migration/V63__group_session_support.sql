-- V63: Group therapy session support.
-- Adds record_kind discriminator to session_record, relaxes client_id to nullable,
-- adds note_scope + target_client_id to session_note, and creates three new tables:
-- session_participant, group_session_attendance, session_participant_audit.

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. New enum type: record_kind
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TYPE record_kind AS ENUM ('INDIVIDUAL', 'GROUP');

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Add record_kind column to session_record (immutable discriminator)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE session_record
    ADD COLUMN record_kind record_kind NOT NULL DEFAULT 'INDIVIDUAL';

-- Back-fill existing rows are INDIVIDUAL (the default already handles this)
-- Make the column non-defaultable going forward (enforced at application layer)

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Relax client_id to nullable (INDIVIDUAL constraint enforced at app layer)
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE session_record
    ALTER COLUMN client_id DROP NOT NULL;

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. New enum type: note_scope
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TYPE note_scope AS ENUM ('SESSION', 'CLIENT');

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Extend session_note with note_scope + target_client_id
-- ─────────────────────────────────────────────────────────────────────────────
ALTER TABLE session_note
    ADD COLUMN note_scope      note_scope NOT NULL DEFAULT 'SESSION',
    ADD COLUMN target_client_id UUID       NULL;

COMMENT ON COLUMN session_note.note_scope IS
    'SESSION = shared group note (stored once); CLIENT = per-client private note';
COMMENT ON COLUMN session_note.target_client_id IS
    'Non-null only when note_scope = CLIENT. Points to the subject client within the group.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 6. session_participant — many-to-many between group session and clients
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE session_participant (
    id               UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_record_id UUID       NOT NULL REFERENCES session_record(id) ON DELETE CASCADE,
    client_id         UUID        NOT NULL,
    joined_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    removed_at        TIMESTAMPTZ NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_session_participant UNIQUE (session_record_id, client_id)
);

-- DB-level cap: max 20 active participants per group session
CREATE OR REPLACE FUNCTION check_participant_cap()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (
        SELECT COUNT(*)
        FROM session_participant
        WHERE session_record_id = NEW.session_record_id
          AND removed_at IS NULL
    ) >= 20 THEN
        RAISE EXCEPTION 'Group session participant cap of 20 exceeded for session %', NEW.session_record_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_check_participant_cap
BEFORE INSERT ON session_participant
FOR EACH ROW EXECUTE FUNCTION check_participant_cap();

CREATE INDEX idx_session_participant_session ON session_participant(session_record_id);
CREATE INDEX idx_session_participant_client ON session_participant(client_id);

COMMENT ON TABLE session_participant IS
    'Links a GROUP session_record to its client participants (≤ 20 active). removed_at records hard-delete audit.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 7. group_session_attendance — per-client attendance for GROUP sessions
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE group_session_attendance (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_record_id  UUID        NOT NULL REFERENCES session_record(id) ON DELETE CASCADE,
    client_id          UUID        NOT NULL,
    attendance_outcome attendance_outcome_type NOT NULL,
    cancelled_at       TIMESTAMPTZ NULL,
    cancellation_initiator_id UUID NULL,
    recorded_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    recorded_by        UUID        NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_group_attendance UNIQUE (session_record_id, client_id)
);

CREATE INDEX idx_group_attendance_session ON group_session_attendance(session_record_id);
CREATE INDEX idx_group_attendance_client ON group_session_attendance(client_id);

COMMENT ON TABLE group_session_attendance IS
    'Per-client attendance outcome for GROUP session records. Separate from session_record.attendance_outcome which is used for INDIVIDUAL sessions.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 8. session_participant_audit — append-only log of participant add/remove
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TYPE participant_audit_action AS ENUM ('ADDED', 'REMOVED');

CREATE TABLE session_participant_audit (
    id                UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    session_record_id  UUID        NOT NULL,
    client_id          UUID        NOT NULL,
    action             participant_audit_action NOT NULL,
    actor_user_id      UUID        NULL,
    actor_name         TEXT        NULL,
    occurred_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_spa_session ON session_participant_audit(session_record_id);
CREATE INDEX idx_spa_client ON session_participant_audit(client_id);

COMMENT ON TABLE session_participant_audit IS
    'Append-only audit log for participant additions and removals. Intentionally has no FK so history is preserved even if session_record is deleted.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 9. Index to support timeline query: find all sessions for a given client
--    including GROUP sessions via session_participant
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX idx_session_record_kind ON session_record(record_kind);
