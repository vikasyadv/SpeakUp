package com.speakup.repository;

import com.speakup.model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByPromptId(Long promptId);

    boolean existsByPromptId(Long promptId);

    void deleteByPromptId(Long promptId);

    List<Bookmark> findAllByOrderByCreatedAtDesc();
}
