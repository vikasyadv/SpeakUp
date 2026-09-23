package com.speakup.service;

import com.speakup.dto.BookmarkDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.CallerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceOwnershipTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookmarkService bookmarkService;

    private User userA;
    private User userB;
    private Prompt prompt1;
    private Bookmark bookmarkUserA;
    private Bookmark bookmarkGuestA;

    @BeforeEach
    void setUp() {
        userA = new User("userA@example.com", "hash", "User A", Role.ROLE_USER);
        userA.setId(201L);

        userB = new User("userB@example.com", "hash", "User B", Role.ROLE_USER);
        userB.setId(202L);

        prompt1 = new Prompt();
        prompt1.setId(10L);
        prompt1.setText("Shared Prompt");
        prompt1.setMode(Mode.OFF_THE_CUFF);
        Category category = new Category("Technology", "Tech topics");
        prompt1.setCategory(category);

        bookmarkUserA = new Bookmark();
        bookmarkUserA.setId(1001L);
        bookmarkUserA.setPrompt(prompt1);
        bookmarkUserA.setUser(userA);
        bookmarkUserA.setGuestId(null);

        bookmarkGuestA = new Bookmark();
        bookmarkGuestA.setId(1002L);
        bookmarkGuestA.setPrompt(prompt1);
        bookmarkGuestA.setUser(null);
        bookmarkGuestA.setGuestId("guest-uuid-A");
    }

    @Test
    @DisplayName("Authenticated user bookmark creates record with user and null guestId")
    void bookmarkPrompt_authenticatedUser_assignsUser() {
        when(promptRepository.findById(10L)).thenReturn(Optional.of(prompt1));
        when(userRepository.findById(201L)).thenReturn(Optional.of(userA));
        when(bookmarkRepository.findByPromptIdAndUser(10L, userA)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        BookmarkDto dto = bookmarkService.bookmarkPrompt(10L, CallerContext.authenticated(201L));

        assertThat(dto).isNotNull();
        ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(captor.capture());
        Bookmark saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(userA);
        assertThat(saved.getGuestId()).isNull();
    }

    @Test
    @DisplayName("Guest bookmark creates record with guestId and null user")
    void bookmarkPrompt_guest_assignsGuestId() {
        when(promptRepository.findById(10L)).thenReturn(Optional.of(prompt1));
        when(bookmarkRepository.findByPromptIdAndGuestId(10L, "guest-uuid-A")).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        BookmarkDto dto = bookmarkService.bookmarkPrompt(10L, CallerContext.guest("guest-uuid-A"));

        assertThat(dto).isNotNull();
        ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(captor.capture());
        Bookmark saved = captor.getValue();
        assertThat(saved.getUser()).isNull();
        assertThat(saved.getGuestId()).isEqualTo("guest-uuid-A");
    }

    @Test
    @DisplayName("Duplicate bookmark by same user is idempotent and returns existing")
    void bookmarkPrompt_duplicateUser_isIdempotent() {
        when(promptRepository.findById(10L)).thenReturn(Optional.of(prompt1));
        when(userRepository.findById(201L)).thenReturn(Optional.of(userA));
        when(bookmarkRepository.findByPromptIdAndUser(10L, userA)).thenReturn(Optional.of(bookmarkUserA));

        BookmarkDto dto = bookmarkService.bookmarkPrompt(10L, CallerContext.authenticated(201L));

        assertThat(dto).isNotNull();
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("Duplicate bookmark by same guest is idempotent and returns existing")
    void bookmarkPrompt_duplicateGuest_isIdempotent() {
        when(promptRepository.findById(10L)).thenReturn(Optional.of(prompt1));
        when(bookmarkRepository.findByPromptIdAndGuestId(10L, "guest-uuid-A")).thenReturn(Optional.of(bookmarkGuestA));

        BookmarkDto dto = bookmarkService.bookmarkPrompt(10L, CallerContext.guest("guest-uuid-A"));

        assertThat(dto).isNotNull();
        verify(bookmarkRepository, never()).save(any());
    }

    @Test
    @DisplayName("User A can bookmark same prompt as User B independently")
    void bookmarkPrompt_multipleUsersCanBookmarkSamePrompt() {
        // User A already bookmarked prompt1
        // Now User B bookmarks prompt1
        when(promptRepository.findById(10L)).thenReturn(Optional.of(prompt1));
        when(userRepository.findById(202L)).thenReturn(Optional.of(userB));
        when(bookmarkRepository.findByPromptIdAndUser(10L, userB)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenAnswer(inv -> inv.getArgument(0));

        BookmarkDto dto = bookmarkService.bookmarkPrompt(10L, CallerContext.authenticated(202L));

        assertThat(dto).isNotNull();
        ArgumentCaptor<Bookmark> captor = ArgumentCaptor.forClass(Bookmark.class);
        verify(bookmarkRepository).save(captor.capture());
        assertThat(captor.getValue().getUser()).isEqualTo(userB);
    }

    @Test
    @DisplayName("removeBookmark for user throws 404 when prompt was bookmarked by another user")
    void removeBookmark_userTriesToDeleteAnotherUsersBookmark_throws404() {
        when(userRepository.findById(202L)).thenReturn(Optional.of(userB));
        // Prompt 10 is bookmarked by User A, but not User B
        when(bookmarkRepository.findByPromptIdAndUser(10L, userB)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.removeBookmark(10L, CallerContext.authenticated(202L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Bookmark not found for prompt id: 10");

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("removeBookmark for guest throws 404 when prompt was bookmarked by another guest")
    void removeBookmark_guestTriesToDeleteAnotherGuestsBookmark_throws404() {
        // Prompt 10 is bookmarked by Guest A, but not Guest B
        when(bookmarkRepository.findByPromptIdAndGuestId(10L, "guest-uuid-B")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookmarkService.removeBookmark(10L, CallerContext.guest("guest-uuid-B")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Bookmark not found for prompt id: 10");

        verify(bookmarkRepository, never()).delete(any());
    }

    @Test
    @DisplayName("isBookmarked is caller-scoped")
    void isBookmarked_callerScoped() {
        when(userRepository.findById(201L)).thenReturn(Optional.of(userA));
        when(userRepository.findById(202L)).thenReturn(Optional.of(userB));
        when(bookmarkRepository.existsByPromptIdAndUser(10L, userA)).thenReturn(true);
        when(bookmarkRepository.existsByPromptIdAndUser(10L, userB)).thenReturn(false);

        assertThat(bookmarkService.isBookmarked(10L, CallerContext.authenticated(201L))).isTrue();
        assertThat(bookmarkService.isBookmarked(10L, CallerContext.authenticated(202L))).isFalse();
    }

    @Test
    @DisplayName("getAllBookmarks returns only caller's bookmarks")
    void getAllBookmarks_callerScoped() {
        when(userRepository.findById(201L)).thenReturn(Optional.of(userA));
        when(bookmarkRepository.findByUserOrderByCreatedAtDesc(userA)).thenReturn(List.of(bookmarkUserA));

        List<BookmarkDto> list = bookmarkService.getAllBookmarks(CallerContext.authenticated(201L));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPrompt().getId()).isEqualTo(10L);
    }
}
