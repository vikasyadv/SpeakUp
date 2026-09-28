package com.speakup.repository;

import com.speakup.model.Session;
import com.speakup.model.SessionStatus;
import com.speakup.model.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    List<Session> findTop20ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    @Query("SELECT s FROM Session s WHERE s.id = :id")
    Optional<Session> findByIdWithPrompt(@Param("id") Long id);

    List<Session> findByGuestIdAndUserIsNull(String guestId);

    List<Session> findByUserOrderByCreatedAtDesc(User user);

    List<Session> findByGuestIdOrderByCreatedAtDesc(String guestId);

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    List<Session> findTop20ByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    List<Session> findTop20ByGuestIdAndUserIsNullOrderByCreatedAtDesc(String guestId);

    @Query("SELECT s FROM Session s WHERE s.user = :user AND s.status = :status ORDER BY COALESCE(s.completedAt, s.createdAt) ASC, s.id ASC")
    List<Session> findByUserAndStatusOrderByChronologicalAsc(@Param("user") User user, @Param("status") SessionStatus status);

    @Query("SELECT s FROM Session s WHERE s.guestId = :guestId AND s.user IS NULL AND s.status = :status ORDER BY COALESCE(s.completedAt, s.createdAt) ASC, s.id ASC")
    List<Session> findByGuestIdAndUserIsNullAndStatusOrderByChronologicalAsc(@Param("guestId") String guestId, @Param("status") SessionStatus status);
}
