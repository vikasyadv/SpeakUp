package com.speakup.service;

import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.dto.SessionDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
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
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceOwnershipTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SessionService sessionService;

    private User userA;
    private User userB;
    private Session sessionUserA;
    private Session sessionUserB;
    private Session sessionGuestA;
    private Session sessionGuestB;

    @BeforeEach
    void setUp() {
        userA = new User("userA@example.com", "hash", "User A", Role.ROLE_USER);
        userA.setId(101L);

        userB = new User("userB@example.com", "hash", "User B", Role.ROLE_USER);
        userB.setId(102L);

        sessionUserA = new Session();
        sessionUserA.setId(1L);
        sessionUserA.setPromptText("User A Topic");
        sessionUserA.setMode(Mode.OFF_THE_CUFF);
        sessionUserA.setDurationSeconds(60);
        sessionUserA.setUser(userA);
        sessionUserA.setGuestId(null);

        sessionUserB = new Session();
        sessionUserB.setId(2L);
        sessionUserB.setPromptText("User B Topic");
        sessionUserB.setMode(Mode.OFF_THE_CUFF);
        sessionUserB.setDurationSeconds(60);
        sessionUserB.setUser(userB);
        sessionUserB.setGuestId(null);

        sessionGuestA = new Session();
        sessionGuestA.setId(3L);
        sessionGuestA.setPromptText("Guest A Topic");
        sessionGuestA.setMode(Mode.OFF_THE_CUFF);
        sessionGuestA.setDurationSeconds(60);
        sessionGuestA.setUser(null);
        sessionGuestA.setGuestId("guest-uuid-A");

        sessionGuestB = new Session();
        sessionGuestB.setId(4L);
        sessionGuestB.setPromptText("Guest B Topic");
        sessionGuestB.setMode(Mode.OFF_THE_CUFF);
        sessionGuestB.setDurationSeconds(60);
        sessionGuestB.setUser(null);
        sessionGuestB.setGuestId("guest-uuid-B");
    }

    @Test
    @DisplayName("Authenticated user can access own session")
    void getById_authenticatedUser_ownsSession_returnsDto() {
        when(sessionRepository.findByIdWithPrompt(1L)).thenReturn(Optional.of(sessionUserA));
        when(feedbackRepository.existsBySessionId(1L)).thenReturn(false);

        SessionDto dto = sessionService.getById(1L, CallerContext.authenticated(101L));

        assertThat(dto).isNotNull();
        assertThat(dto.getPromptText()).isEqualTo("User A Topic");
    }

    @Test
    @DisplayName("Authenticated user cannot access another user's session (returns 404)")
    void getById_authenticatedUser_doesNotOwnSession_throws404() {
        when(sessionRepository.findByIdWithPrompt(2L)).thenReturn(Optional.of(sessionUserB));

        assertThatThrownBy(() -> sessionService.getById(2L, CallerContext.authenticated(101L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 2");
    }

    @Test
    @DisplayName("Authenticated user cannot access a guest's session (returns 404)")
    void getById_authenticatedUser_accessingGuestSession_throws404() {
        when(sessionRepository.findByIdWithPrompt(3L)).thenReturn(Optional.of(sessionGuestA));

        assertThatThrownBy(() -> sessionService.getById(3L, CallerContext.authenticated(101L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 3");
    }

    @Test
    @DisplayName("Guest can access own session")
    void getById_guest_ownsSession_returnsDto() {
        when(sessionRepository.findByIdWithPrompt(3L)).thenReturn(Optional.of(sessionGuestA));
        when(feedbackRepository.existsBySessionId(3L)).thenReturn(false);

        SessionDto dto = sessionService.getById(3L, CallerContext.guest("guest-uuid-A"));

        assertThat(dto).isNotNull();
        assertThat(dto.getPromptText()).isEqualTo("Guest A Topic");
    }

    @Test
    @DisplayName("Guest cannot access another guest's session (returns 404)")
    void getById_guest_accessingOtherGuestSession_throws404() {
        when(sessionRepository.findByIdWithPrompt(4L)).thenReturn(Optional.of(sessionGuestB));

        assertThatThrownBy(() -> sessionService.getById(4L, CallerContext.guest("guest-uuid-A")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 4");
    }

    @Test
    @DisplayName("Guest cannot access authenticated user's session (returns 404)")
    void getById_guest_accessingUserSession_throws404() {
        when(sessionRepository.findByIdWithPrompt(1L)).thenReturn(Optional.of(sessionUserA));

        assertThatThrownBy(() -> sessionService.getById(1L, CallerContext.guest("guest-uuid-A")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 1");
    }

    @Test
    @DisplayName("Session creation assigns User and null guestId when authenticated")
    void createSession_authenticatedUser_assignsUserAndNullGuestId() {
        SessionCreateDto dto = new SessionCreateDto("Prompt test", null, "OFF_THE_CUFF", 60);
        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionService.createSession(dto, CallerContext.authenticated(101L));

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(userA);
        assertThat(saved.getGuestId()).isNull();
    }

    @Test
    @DisplayName("Session creation assigns guestId and null user when guest")
    void createSession_guest_assignsGuestIdAndNullUser() {
        SessionCreateDto dto = new SessionCreateDto("Prompt test", null, "OFF_THE_CUFF", 60);
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        sessionService.createSession(dto, CallerContext.guest("guest-uuid-A"));

        ArgumentCaptor<Session> captor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(captor.capture());
        Session saved = captor.getValue();
        assertThat(saved.getUser()).isNull();
        assertThat(saved.getGuestId()).isEqualTo("guest-uuid-A");
    }

    @Test
    @DisplayName("completeSession throws 404 when caller does not own the session")
    void completeSession_ownershipMismatch_throws404() {
        when(sessionRepository.findByIdWithPrompt(2L)).thenReturn(Optional.of(sessionUserB));

        SessionCompleteDto completeDto = new SessionCompleteDto("Transcript", 55);

        assertThatThrownBy(() -> sessionService.completeSession(2L, completeDto, CallerContext.authenticated(101L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 2");

        verify(sessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteSession throws 404 when caller does not own the session")
    void deleteSession_ownershipMismatch_throws404() {
        when(sessionRepository.findByIdWithPrompt(2L)).thenReturn(Optional.of(sessionUserB));

        assertThatThrownBy(() -> sessionService.deleteSession(2L, CallerContext.authenticated(101L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 2");

        verify(sessionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getRecentSessions returns only authenticated user's sessions")
    void getRecentSessions_authenticatedUser_returnsUserSessions() {
        when(userRepository.findById(101L)).thenReturn(Optional.of(userA));
        when(sessionRepository.findTop20ByUserOrderByCreatedAtDesc(userA)).thenReturn(List.of(sessionUserA));
        when(feedbackRepository.findSessionIdsWithFeedback(List.of(1L))).thenReturn(Set.of());

        List<SessionDto> list = sessionService.getRecentSessions(CallerContext.authenticated(101L));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPromptText()).isEqualTo("User A Topic");
    }

    @Test
    @DisplayName("getRecentSessions returns only guest's sessions")
    void getRecentSessions_guest_returnsGuestSessions() {
        when(sessionRepository.findTop20ByGuestIdAndUserIsNullOrderByCreatedAtDesc("guest-uuid-A"))
                .thenReturn(List.of(sessionGuestA));
        when(feedbackRepository.findSessionIdsWithFeedback(List.of(3L))).thenReturn(Set.of());

        List<SessionDto> list = sessionService.getRecentSessions(CallerContext.guest("guest-uuid-A"));

        assertThat(list).hasSize(1);
        assertThat(list.get(0).getPromptText()).isEqualTo("Guest A Topic");
    }
}
