package com.speakup.repository;

import com.speakup.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findAllByOrderByCreatedAtDesc();

    List<Session> findTop20ByOrderByCreatedAtDesc();
}
