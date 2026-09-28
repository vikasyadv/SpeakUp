package com.speakup.repository;

import com.speakup.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    Optional<Feedback> findBySessionId(Long sessionId);

    boolean existsBySessionId(Long sessionId);

    @Query("SELECT f.session.id FROM Feedback f WHERE f.session.id IN :sessionIds")
    Set<Long> findSessionIdsWithFeedback(@Param("sessionIds") Collection<Long> sessionIds);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.session WHERE f.session.id IN :sessionIds")
    List<Feedback> findBySessionIdInWithSession(@Param("sessionIds") Collection<Long> sessionIds);
}
