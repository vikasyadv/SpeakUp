package com.speakup.service;

import com.speakup.dto.BookmarkDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.BookmarkMapper;
import com.speakup.model.Bookmark;
import com.speakup.model.Prompt;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.PromptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PromptRepository promptRepository;

    public BookmarkService(BookmarkRepository bookmarkRepository, PromptRepository promptRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.promptRepository = promptRepository;
    }

    /**
     * Bookmark a prompt. If already bookmarked, return the existing bookmark.
     */
    @Transactional
    public BookmarkDto bookmarkPrompt(Long promptId) {
        // Check if already bookmarked
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

    /**
     * Remove a bookmark by prompt ID.
     */
    @Transactional
    public void removeBookmark(Long promptId) {
        if (!bookmarkRepository.existsByPromptId(promptId)) {
            throw new ResourceNotFoundException("Bookmark not found for prompt id: " + promptId);
        }
        bookmarkRepository.deleteByPromptId(promptId);
    }

    /**
     * Check if a prompt is bookmarked.
     */
    public boolean isBookmarked(Long promptId) {
        return bookmarkRepository.existsByPromptId(promptId);
    }

    /**
     * Get all bookmarks, most recent first.
     */
    public List<BookmarkDto> getAllBookmarks() {
        return bookmarkRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(BookmarkMapper::toDto)
                .toList();
    }
}
