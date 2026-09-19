package com.speakup.repository;

import com.speakup.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);

    @Query("SELECT f.session.id FROM Feedback f WHERE f.session.id IN :sessionIds")
    Set<Long> findSessionIdsWithFeedback(@Param("sessionIds") Collection<Long> sessionIds);
}
