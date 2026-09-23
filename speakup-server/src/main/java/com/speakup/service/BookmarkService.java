package com.speakup.service;

import com.speakup.dto.BookmarkDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.BookmarkMapper;
import com.speakup.model.Bookmark;
import com.speakup.model.Prompt;
import com.speakup.model.User;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.CallerContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PromptRepository promptRepository;
    private final UserRepository userRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository,
                           PromptRepository promptRepository,
                           UserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.promptRepository = promptRepository;
        this.userRepository = userRepository;
    }

    /**
     * Bookmark a prompt for the current caller (authenticated user or guest).
     * Scoped to the caller; duplicate bookmarks by the same caller are idempotent.
     */
    @Transactional
    public BookmarkDto bookmarkPrompt(Long promptId, CallerContext caller) {
        if (caller != null && caller.isAuthenticated()) {
            Prompt prompt = promptRepository.findById(promptId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
            User user = userRepository.findById(caller.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + caller.getUserId()));

            Optional<Bookmark> existing = bookmarkRepository.findByPromptIdAndUser(promptId, user);
            if (existing.isPresent()) {
                return BookmarkMapper.toDto(existing.get());
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setPrompt(prompt);
            bookmark.setUser(user);
            bookmark.setGuestId(null);
            Bookmark saved = bookmarkRepository.save(bookmark);
            return BookmarkMapper.toDto(saved);
        }

        if (caller != null && caller.isGuest()) {
            Prompt prompt = promptRepository.findById(promptId)
                    .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
            Optional<Bookmark> existing = bookmarkRepository.findByPromptIdAndGuestId(promptId, caller.getGuestId());
            if (existing.isPresent()) {
                return BookmarkMapper.toDto(existing.get());
            }

            Bookmark bookmark = new Bookmark();
            bookmark.setPrompt(prompt);
            bookmark.setUser(null);
            bookmark.setGuestId(caller.getGuestId());
            Bookmark saved = bookmarkRepository.save(bookmark);
            return BookmarkMapper.toDto(saved);
        }

        // Anonymous legacy fallback
        return bookmarkRepository.findByPromptId(promptId)
                .map(BookmarkMapper::toDto)
                .orElseGet(() -> {
                    Prompt prompt = promptRepository.findById(promptId)
                            .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + promptId));
                    Bookmark bookmark = new Bookmark();
                    bookmark.setPrompt(prompt);
                    Bookmark saved = bookmarkRepository.save(bookmark);
                    return BookmarkMapper.toDto(saved);
                });
    }

    public BookmarkDto bookmarkPrompt(Long promptId) {
        return bookmarkPrompt(promptId, CallerContext.anonymous());
    }

    /**
     * Remove a bookmark for the current caller by prompt ID.
     * Throws 404 if not found or not owned by this caller.
     */
    @Transactional
    public void removeBookmark(Long promptId, CallerContext caller) {
        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + caller.getUserId()));
            Bookmark bookmark = bookmarkRepository.findByPromptIdAndUser(promptId, user)
                    .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found for prompt id: " + promptId));
            bookmarkRepository.delete(bookmark);
            return;
        }

        if (caller != null && caller.isGuest()) {
            Bookmark bookmark = bookmarkRepository.findByPromptIdAndGuestId(promptId, caller.getGuestId())
                    .orElseThrow(() -> new ResourceNotFoundException("Bookmark not found for prompt id: " + promptId));
            bookmarkRepository.delete(bookmark);
            return;
        }

        // Anonymous legacy fallback
        if (!bookmarkRepository.existsByPromptId(promptId)) {
            throw new ResourceNotFoundException("Bookmark not found for prompt id: " + promptId);
        }
        bookmarkRepository.deleteByPromptId(promptId);
    }

    public void removeBookmark(Long promptId) {
        removeBookmark(promptId, CallerContext.anonymous());
    }

    /**
     * Check if a prompt is bookmarked by the current caller.
     */
    @Transactional(readOnly = true)
    public boolean isBookmarked(Long promptId, CallerContext caller) {
        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId()).orElse(null);
            return user != null && bookmarkRepository.existsByPromptIdAndUser(promptId, user);
        }

        if (caller != null && caller.isGuest()) {
            return bookmarkRepository.existsByPromptIdAndGuestId(promptId, caller.getGuestId());
        }

        return bookmarkRepository.existsByPromptId(promptId);
    }

    public boolean isBookmarked(Long promptId) {
        return isBookmarked(promptId, CallerContext.anonymous());
    }

    /**
     * Get all bookmarks for the current caller, most recent first.
     */
    @Transactional(readOnly = true)
    public List<BookmarkDto> getAllBookmarks(CallerContext caller) {
        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId()).orElse(null);
            if (user == null) {
                return List.of();
            }
            return bookmarkRepository.findByUserOrderByCreatedAtDesc(user).stream()
                    .map(BookmarkMapper::toDto)
                    .toList();
        }

        if (caller != null && caller.isGuest()) {
            return bookmarkRepository.findByGuestIdOrderByCreatedAtDesc(caller.getGuestId()).stream()
                    .map(BookmarkMapper::toDto)
                    .toList();
        }

        return bookmarkRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BookmarkMapper::toDto)
                .toList();
    }

    public List<BookmarkDto> getAllBookmarks() {
        return getAllBookmarks(CallerContext.anonymous());
    }
}
