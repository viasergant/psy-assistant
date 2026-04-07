package com.psyassistant.sessions.repository;

import com.psyassistant.sessions.domain.NoteTemplate;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for {@link NoteTemplate} — system-defined structured note templates.
 */
@Repository
public interface NoteTemplateRepository extends JpaRepository<NoteTemplate, UUID> {
}
