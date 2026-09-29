package com.speakup.repository;

import com.speakup.model.Mode;
import com.speakup.model.Session;
import com.speakup.model.SessionStatus;
import com.speakup.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @EntityGraph(attributePaths = {"prompt"})
    @Query("SELECT s FROM Session s WHERE s.user = :user AND s.status = :status ORDER BY COALESCE(s.completedAt, s.createdAt) ASC, s.id ASC")
    List<Session> findByUserAndStatusOrderByChronologicalAsc(@Param("user") User user, @Param("status") SessionStatus status);

    @EntityGraph(attributePaths = {"prompt"})
    @Query("SELECT s FROM Session s WHERE s.guestId = :guestId AND s.user IS NULL AND s.status = :status ORDER BY COALESCE(s.completedAt, s.createdAt) ASC, s.id ASC")
    List<Session> findByGuestIdAndUserIsNullAndStatusOrderByChronologicalAsc(@Param("guestId") String guestId, @Param("status") SessionStatus status);

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    @Query(value = "SELECT s FROM Session s " +
                   "LEFT JOIN s.prompt p " +
                   "LEFT JOIN p.category c " +
                   "WHERE s.user = :user " +
                   "AND (:mode IS NULL OR s.mode = :mode) " +
                   "AND (:status IS NULL OR s.status = :status) " +
                   "AND (:search IS NULL OR (" +
                   "     LOWER(s.promptText) LIKE :search " +
                   "     OR LOWER(s.transcript) LIKE :search " +
                   "     OR LOWER(s.preparationNotes) LIKE :search " +
                   "     OR LOWER(c.name) LIKE :search))",
           countQuery = "SELECT count(s) FROM Session s " +
                        "LEFT JOIN s.prompt p " +
                        "LEFT JOIN p.category c " +
                        "WHERE s.user = :user " +
                        "AND (:mode IS NULL OR s.mode = :mode) " +
                        "AND (:status IS NULL OR s.status = :status) " +
                        "AND (:search IS NULL OR (" +
                        "     LOWER(s.promptText) LIKE :search " +
                        "     OR LOWER(s.transcript) LIKE :search " +
                        "     OR LOWER(s.preparationNotes) LIKE :search " +
                        "     OR LOWER(c.name) LIKE :search))")
    Page<Session> findByUserWithFilters(
            @Param("user") User user,
            @Param("mode") Mode mode,
            @Param("status") SessionStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    @Query(value = "SELECT s FROM Session s " +
                   "LEFT JOIN s.prompt p " +
                   "LEFT JOIN p.category c " +
                   "WHERE s.guestId = :guestId AND s.user IS NULL " +
                   "AND (:mode IS NULL OR s.mode = :mode) " +
                   "AND (:status IS NULL OR s.status = :status) " +
                   "AND (:search IS NULL OR (" +
                   "     LOWER(s.promptText) LIKE :search " +
                   "     OR LOWER(s.transcript) LIKE :search " +
                   "     OR LOWER(s.preparationNotes) LIKE :search " +
                   "     OR LOWER(c.name) LIKE :search))",
           countQuery = "SELECT count(s) FROM Session s " +
                        "LEFT JOIN s.prompt p " +
                        "LEFT JOIN p.category c " +
                        "WHERE s.guestId = :guestId AND s.user IS NULL " +
                        "AND (:mode IS NULL OR s.mode = :mode) " +
                        "AND (:status IS NULL OR s.status = :status) " +
                        "AND (:search IS NULL OR (" +
                        "     LOWER(s.promptText) LIKE :search " +
                        "     OR LOWER(s.transcript) LIKE :search " +
                        "     OR LOWER(s.preparationNotes) LIKE :search " +
                        "     OR LOWER(c.name) LIKE :search))")
    Page<Session> findByGuestWithFilters(
            @Param("guestId") String guestId,
            @Param("mode") Mode mode,
            @Param("status") SessionStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"prompt", "prompt.category"})
    @Query(value = "SELECT s FROM Session s " +
                   "LEFT JOIN s.prompt p " +
                   "LEFT JOIN p.category c " +
                   "WHERE s.guestId IS NULL AND s.user IS NULL " +
                   "AND (:mode IS NULL OR s.mode = :mode) " +
                   "AND (:status IS NULL OR s.status = :status) " +
                   "AND (:search IS NULL OR (" +
                   "     LOWER(s.promptText) LIKE :search " +
                   "     OR LOWER(s.transcript) LIKE :search " +
                   "     OR LOWER(s.preparationNotes) LIKE :search " +
                   "     OR LOWER(c.name) LIKE :search))",
           countQuery = "SELECT count(s) FROM Session s " +
                        "LEFT JOIN s.prompt p " +
                        "LEFT JOIN p.category c " +
                        "WHERE s.guestId IS NULL AND s.user IS NULL " +
                        "AND (:mode IS NULL OR s.mode = :mode) " +
                        "AND (:status IS NULL OR s.status = :status) " +
                        "AND (:search IS NULL OR (" +
                        "     LOWER(s.promptText) LIKE :search " +
                        "     OR LOWER(s.transcript) LIKE :search " +
                        "     OR LOWER(s.preparationNotes) LIKE :search " +
                        "     OR LOWER(c.name) LIKE :search))")
    Page<Session> findAnonymousWithFilters(
            @Param("mode") Mode mode,
            @Param("status") SessionStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
