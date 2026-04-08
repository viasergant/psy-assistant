package com.psyassistant.careplans.repository;

import com.psyassistant.careplans.domain.GoalProgressNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Append-only repository for {@link GoalProgressNote} entries. */
@Repository
public interface GoalProgressNoteRepository extends JpaRepository<GoalProgressNote, UUID> {

    List<GoalProgressNote> findByGoalIdOrderByCreatedAtDesc(UUID goalId);

    List<GoalProgressNote> findByGoalIdAndAuthorUserIdOrderByCreatedAtDesc(
            UUID goalId, UUID authorUserId);
}
