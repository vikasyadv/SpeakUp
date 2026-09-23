package com.speakup.repository;

import com.speakup.model.Bookmark;
import com.speakup.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Optional<Bookmark> findByPromptId(Long promptId);

    boolean existsByPromptId(Long promptId);

    void deleteByPromptId(Long promptId);

    List<Bookmark> findAllByOrderByCreatedAtDesc();

    List<Bookmark> findByGuestIdAndUserIsNull(String guestId);

    List<Bookmark> findByUserOrderByCreatedAtDesc(User user);

    List<Bookmark> findByGuestIdOrderByCreatedAtDesc(String guestId);

    boolean existsByPromptIdAndUser(Long promptId, User user);

    boolean existsByPromptIdAndGuestId(Long promptId, String guestId);

    Optional<Bookmark> findByPromptIdAndUser(Long promptId, User user);

    Optional<Bookmark> findByPromptIdAndGuestId(Long promptId, String guestId);
}
