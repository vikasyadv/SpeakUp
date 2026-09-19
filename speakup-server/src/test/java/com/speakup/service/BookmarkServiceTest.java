package com.speakup.service;

import com.speakup.dto.BookmarkDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PromptRepository promptRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    private Prompt samplePrompt;
    private Bookmark sampleBookmark;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category("Technology", "Tech topics");
        sampleCategory.setId(1L);

        samplePrompt = new Prompt();
        samplePrompt.setId(1L);
        samplePrompt.setText("Artificial Intelligence");
        samplePrompt.setCategory(sampleCategory);
        samplePrompt.setMode(Mode.OFF_THE_CUFF);
        samplePrompt.setActive(true);

        sampleBookmark = new Bookmark();
        sampleBookmark.setId(1L);
        sampleBookmark.setPrompt(samplePrompt);
        sampleBookmark.setCreatedAt(Instant.now());
    }

    @Test
    void bookmarkPrompt_createsNewBookmark() {
        when(bookmarkRepository.findByPromptId(1L)).thenReturn(Optional.empty());
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(sampleBookmark);

        BookmarkDto result = bookmarkService.bookmarkPrompt(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPrompt().getId());
        assertEquals("Artificial Intelligence", result.getPrompt().getText());
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    void bookmarkPrompt_returnsExistingWhenDuplicate() {
        when(bookmarkRepository.findByPromptId(1L)).thenReturn(Optional.of(sampleBookmark));

        BookmarkDto result = bookmarkService.bookmarkPrompt(1L);

        assertNotNull(result);
        assertEquals(1L, result.getPrompt().getId());
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    void bookmarkPrompt_throwsWhenPromptNotFound() {
        when(bookmarkRepository.findByPromptId(99L)).thenReturn(Optional.empty());
        when(promptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> bookmarkService.bookmarkPrompt(99L));
    }

    @Test
    void removeBookmark_deletesWhenExists() {
        when(bookmarkRepository.existsByPromptId(1L)).thenReturn(true);

        bookmarkService.removeBookmark(1L);

        verify(bookmarkRepository).deleteByPromptId(1L);
    }

    @Test
    void removeBookmark_throwsWhenNotFound() {
        when(bookmarkRepository.existsByPromptId(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> bookmarkService.removeBookmark(99L));
    }

    @Test
    void isBookmarked_returnsTrueWhenExists() {
        when(bookmarkRepository.existsByPromptId(1L)).thenReturn(true);

        assertTrue(bookmarkService.isBookmarked(1L));
    }

    @Test
    void isBookmarked_returnsFalseWhenNotExists() {
        when(bookmarkRepository.existsByPromptId(99L)).thenReturn(false);

        assertFalse(bookmarkService.isBookmarked(99L));
    }

    @Test
    void getAllBookmarks_returnsList() {
        when(bookmarkRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleBookmark));

        List<BookmarkDto> result = bookmarkService.getAllBookmarks();

        assertEquals(1, result.size());
        assertEquals("Artificial Intelligence", result.get(0).getPrompt().getText());
    }

    @Test
    void getAllBookmarks_returnsEmptyList() {
        when(bookmarkRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of());

        List<BookmarkDto> result = bookmarkService.getAllBookmarks();

        assertTrue(result.isEmpty());
    }
}
