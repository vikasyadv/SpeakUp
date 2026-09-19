package com.speakup.repository;

import com.speakup.model.Session;
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
}
