package com.speakup.service;

import com.speakup.dto.FeedbackDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.security.CallerContext;
import com.speakup.service.ai.AiSpeakingCoachClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceOwnershipTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AiSpeakingCoachClient aiSpeakingCoachClient;

    @InjectMocks
    private FeedbackService feedbackService;

    private User userA;
    private User userB;
    private Session sessionUserA;
    private Session sessionUserB;
    private Session sessionGuestA;
    private Session sessionGuestB;
    private Feedback feedbackUserA;
    private Feedback feedbackGuestA;

    @BeforeEach
    void setUp() {
        userA = new User("userA@example.com", "hash", "User A", Role.ROLE_USER);
        userA.setId(301L);

        userB = new User("userB@example.com", "hash", "User B", Role.ROLE_USER);
        userB.setId(302L);

        sessionUserA = new Session();
        sessionUserA.setId(50L);
        sessionUserA.setMode(Mode.OFF_THE_CUFF);
        sessionUserA.setPromptText("Topic A");
        sessionUserA.setStatus(SessionStatus.COMPLETED);
        sessionUserA.setTranscript("This is a complete five word transcript");
        sessionUserA.setDurationSeconds(60);
        sessionUserA.setUser(userA);
        sessionUserA.setGuestId(null);

        sessionUserB = new Session();
        sessionUserB.setId(51L);
        sessionUserB.setMode(Mode.OFF_THE_CUFF);
        sessionUserB.setPromptText("Topic B");
        sessionUserB.setStatus(SessionStatus.COMPLETED);
        sessionUserB.setTranscript("This is a complete five word transcript");
        sessionUserB.setDurationSeconds(60);
        sessionUserB.setUser(userB);
        sessionUserB.setGuestId(null);

        sessionGuestA = new Session();
        sessionGuestA.setId(52L);
        sessionGuestA.setMode(Mode.OFF_THE_CUFF);
        sessionGuestA.setPromptText("Topic GA");
        sessionGuestA.setStatus(SessionStatus.COMPLETED);
        sessionGuestA.setTranscript("This is a complete five word transcript");
        sessionGuestA.setDurationSeconds(60);
        sessionGuestA.setUser(null);
        sessionGuestA.setGuestId("guest-uuid-A");

        sessionGuestB = new Session();
        sessionGuestB.setId(53L);
        sessionGuestB.setMode(Mode.OFF_THE_CUFF);
        sessionGuestB.setPromptText("Topic GB");
        sessionGuestB.setStatus(SessionStatus.COMPLETED);
        sessionGuestB.setTranscript("This is a complete five word transcript");
        sessionGuestB.setDurationSeconds(60);
        sessionGuestB.setUser(null);
        sessionGuestB.setGuestId("guest-uuid-B");

        feedbackUserA = new Feedback();
        feedbackUserA.setId(1L);
        feedbackUserA.setSession(sessionUserA);
        feedbackUserA.setOverallScore(8);
        feedbackUserA.setSummary("Great delivery");

        feedbackGuestA = new Feedback();
        feedbackGuestA.setId(2L);
        feedbackGuestA.setSession(sessionGuestA);
        feedbackGuestA.setOverallScore(7);
        feedbackGuestA.setSummary("Good pace");
    }

    @Test
    @DisplayName("Authenticated user can access feedback for own session")
    void getFeedbackBySessionId_authenticatedUser_ownsSession_returnsDto() {
        when(sessionRepository.findById(50L)).thenReturn(Optional.of(sessionUserA));
        when(feedbackRepository.findBySessionId(50L)).thenReturn(Optional.of(feedbackUserA));

        FeedbackDto dto = feedbackService.getFeedbackBySessionId(50L, CallerContext.authenticated(301L));

        assertThat(dto).isNotNull();
        assertThat(dto.getSummary()).isEqualTo("Great delivery");
    }

    @Test
    @DisplayName("Authenticated user cannot access feedback for another user's session (throws 404)")
    void getFeedbackBySessionId_authenticatedUser_doesNotOwnSession_throws404() {
        when(sessionRepository.findById(51L)).thenReturn(Optional.of(sessionUserB));

        assertThatThrownBy(() -> feedbackService.getFeedbackBySessionId(51L, CallerContext.authenticated(301L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 51");
    }

    @Test
    @DisplayName("Guest can access feedback for own session")
    void getFeedbackBySessionId_guest_ownsSession_returnsDto() {
        when(sessionRepository.findById(52L)).thenReturn(Optional.of(sessionGuestA));
        when(feedbackRepository.findBySessionId(52L)).thenReturn(Optional.of(feedbackGuestA));

        FeedbackDto dto = feedbackService.getFeedbackBySessionId(52L, CallerContext.guest("guest-uuid-A"));

        assertThat(dto).isNotNull();
        assertThat(dto.getSummary()).isEqualTo("Good pace");
    }

    @Test
    @DisplayName("Guest cannot access feedback for another guest's session (throws 404)")
    void getFeedbackBySessionId_guest_accessingOtherGuestFeedback_throws404() {
        when(sessionRepository.findById(53L)).thenReturn(Optional.of(sessionGuestB));

        assertThatThrownBy(() -> feedbackService.getFeedbackBySessionId(53L, CallerContext.guest("guest-uuid-A")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 53");
    }

    @Test
    @DisplayName("Guest cannot access feedback for authenticated user's session (throws 404)")
    void getFeedbackBySessionId_guest_accessingUserFeedback_throws404() {
        when(sessionRepository.findById(50L)).thenReturn(Optional.of(sessionUserA));

        assertThatThrownBy(() -> feedbackService.getFeedbackBySessionId(50L, CallerContext.guest("guest-uuid-A")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 50");
    }

    @Test
    @DisplayName("generateOrGetFeedback throws 404 when caller does not own the parent session")
    void generateOrGetFeedback_ownershipMismatch_throws404() {
        when(sessionRepository.findById(51L)).thenReturn(Optional.of(sessionUserB));

        assertThatThrownBy(() -> feedbackService.generateOrGetFeedback(51L, CallerContext.authenticated(301L)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Session not found with id: 51");

        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
    }
}
