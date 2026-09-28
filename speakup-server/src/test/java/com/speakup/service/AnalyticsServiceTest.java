package com.speakup.service;

import com.speakup.dto.*;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.CallerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private User testUser;
    private final String testGuestId = "guest-uuid-123";

    @BeforeEach
    void setUp() {
        testUser = new User("alice@example.com", "hash", "Alice", Role.ROLE_USER);
        testUser.setId(10L);
    }

    @Test
    @DisplayName("Anonymous caller receives empty baseline statistics")
    void anonymousCaller_returnsEmptyDashboard() {
        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.anonymous());

        assertThat(result).isNotNull();
        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(0);
        assertThat(result.getSummary().getTotalSpeakingTimeSeconds()).isEqualTo(0);
        assertThat(result.getSummary().getAverageOverallScore()).isNull();
        assertThat(result.getSummary().getReviewedSessionsCount()).isEqualTo(0);
        assertThat(result.getSkills().getAverageClarity()).isNull();
        assertThat(result.getSkills().getAverageRelevance()).isNull();
        assertThat(result.getSkills().getAverageStructure()).isNull();
        assertThat(result.getModeBreakdown().getOffTheCuffCount()).isEqualTo(0);
        assertThat(result.getModeBreakdown().getResearchCount()).isEqualTo(0);
        assertThat(result.getModeBreakdown().getDebateCount()).isEqualTo(0);
        assertThat(result.getModeBreakdown().getStoryCount()).isEqualTo(0);
        assertThat(result.getScoreHistory()).isEmpty();
        assertThat(result.getRecentActivity()).isEmpty();

        verifyNoInteractions(sessionRepository, feedbackRepository);
    }

    @Test
    @DisplayName("Authenticated caller with no completed sessions returns empty dashboard")
    void authenticatedCaller_noSessions_returnsEmptyDashboard() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(sessionRepository.findByUserAndStatusOrderByChronologicalAsc(testUser, SessionStatus.COMPLETED))
                .thenReturn(List.of());

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.authenticated(10L));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(0);
        assertThat(result.getScoreHistory()).isEmpty();
        assertThat(result.getRecentActivity()).isEmpty();
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("Authenticated caller when user does not exist in repository returns empty dashboard")
    void authenticatedCaller_userNotFound_returnsEmptyDashboard() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.authenticated(999L));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(0);
        verify(sessionRepository, never()).findByUserAndStatusOrderByChronologicalAsc(any(), any());
    }

    @Test
    @DisplayName("Guest caller with no completed sessions returns empty dashboard")
    void guestCaller_noSessions_returnsEmptyDashboard() {
        when(sessionRepository.findByGuestIdAndUserIsNullAndStatusOrderByChronologicalAsc(testGuestId, SessionStatus.COMPLETED))
                .thenReturn(List.of());

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.guest(testGuestId));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(0);
        assertThat(result.getScoreHistory()).isEmpty();
        verifyNoInteractions(feedbackRepository);
    }

    @Test
    @DisplayName("Authenticated caller with completed sessions and feedback computes accurate progress metrics")
    void authenticatedCaller_withSessionsAndFeedback_computesMetrics() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        Instant t1 = Instant.parse("2026-09-20T10:00:00Z");
        Instant t2 = Instant.parse("2026-09-21T10:00:00Z");
        Instant t3 = Instant.parse("2026-09-22T10:00:00Z");
        Instant t4 = Instant.parse("2026-09-23T10:00:00Z");

        // Session 1: OFF_THE_CUFF, actual duration 65, duration 60, has feedback
        Session s1 = createSession(1L, testUser, null, Mode.OFF_THE_CUFF, 60, 65, t1, "Prompt 1");
        // Session 2: RESEARCH, actual duration 120, duration 120, has feedback
        Session s2 = createSession(2L, testUser, null, Mode.RESEARCH, 120, 120, t2, "Prompt 2");
        // Session 3: DEBATE, actual duration null (defaults to duration 90), no feedback
        Session s3 = createSession(3L, testUser, null, Mode.DEBATE, 90, null, t3, "Prompt 3");
        // Session 4: STORY, actual duration 110, duration 120, has feedback
        Session s4 = createSession(4L, testUser, null, Mode.STORY, 120, 110, t4, "Prompt 4");

        List<Session> sessions = List.of(s1, s2, s3, s4);
        when(sessionRepository.findByUserAndStatusOrderByChronologicalAsc(testUser, SessionStatus.COMPLETED))
                .thenReturn(sessions);

        Feedback f1 = createFeedback(101L, s1, 80, 85, 75, 80);
        Feedback f2 = createFeedback(102L, s2, 90, 95, 85, 90);
        Feedback f4 = createFeedback(104L, s4, 85, 80, 90, 85);

        when(feedbackRepository.findBySessionIdInWithSession(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(List.of(f1, f2, f4));

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.authenticated(10L));

        // Total completed sessions = 4
        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(4);
        // Total speaking time = 65 + 120 + 90 + 110 = 385 seconds
        assertThat(result.getSummary().getTotalSpeakingTimeSeconds()).isEqualTo(385);
        // Reviewed sessions count = 3
        assertThat(result.getSummary().getReviewedSessionsCount()).isEqualTo(3);
        // Overall average = (80 + 90 + 85) / 3 = 85.0
        assertThat(result.getSummary().getAverageOverallScore()).isEqualTo(85.0);

        // Skills:
        // Clarity = (85 + 95 + 80) / 3 = 260 / 3 = 86.666... -> 86.7
        assertThat(result.getSkills().getAverageClarity()).isEqualTo(86.7);
        // Relevance = (75 + 85 + 90) / 3 = 250 / 3 = 83.333... -> 83.3
        assertThat(result.getSkills().getAverageRelevance()).isEqualTo(83.3);
        // Structure = (80 + 90 + 85) / 3 = 85.0
        assertThat(result.getSkills().getAverageStructure()).isEqualTo(85.0);

        // Mode breakdown
        assertThat(result.getModeBreakdown().getOffTheCuffCount()).isEqualTo(1);
        assertThat(result.getModeBreakdown().getResearchCount()).isEqualTo(1);
        assertThat(result.getModeBreakdown().getDebateCount()).isEqualTo(1);
        assertThat(result.getModeBreakdown().getStoryCount()).isEqualTo(1);

        // Score history: 3 points (s1, s2, s4), strictly chronological ASC
        assertThat(result.getScoreHistory()).hasSize(3);
        assertThat(result.getScoreHistory().get(0).getSessionId()).isEqualTo(1L);
        assertThat(result.getScoreHistory().get(0).getOverallScore()).isEqualTo(80);
        assertThat(result.getScoreHistory().get(0).getMode()).isEqualTo("OFF_THE_CUFF");
        assertThat(result.getScoreHistory().get(1).getSessionId()).isEqualTo(2L);
        assertThat(result.getScoreHistory().get(1).getOverallScore()).isEqualTo(90);
        assertThat(result.getScoreHistory().get(2).getSessionId()).isEqualTo(4L);
        assertThat(result.getScoreHistory().get(2).getOverallScore()).isEqualTo(85);

        // Recent activity: 4 sessions, ordered DESC by completedAt
        assertThat(result.getRecentActivity()).hasSize(4);
        assertThat(result.getRecentActivity().get(0).getSessionId()).isEqualTo(4L);
        assertThat(result.getRecentActivity().get(0).isHasFeedback()).isTrue();
        assertThat(result.getRecentActivity().get(0).getOverallScore()).isEqualTo(85);

        assertThat(result.getRecentActivity().get(1).getSessionId()).isEqualTo(3L);
        assertThat(result.getRecentActivity().get(1).isHasFeedback()).isFalse();
        assertThat(result.getRecentActivity().get(1).getOverallScore()).isNull();

        assertThat(result.getRecentActivity().get(2).getSessionId()).isEqualTo(2L);
        assertThat(result.getRecentActivity().get(3).getSessionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Guest caller computes metrics scoped to guestId")
    void guestCaller_computesMetrics() {
        Instant t1 = Instant.parse("2026-09-24T12:00:00Z");
        Session s1 = createSession(50L, null, testGuestId, Mode.DEBATE, 180, 175, t1, "Guest Prompt");
        Feedback f1 = createFeedback(201L, s1, 92, 90, 94, 92);

        when(sessionRepository.findByGuestIdAndUserIsNullAndStatusOrderByChronologicalAsc(testGuestId, SessionStatus.COMPLETED))
                .thenReturn(List.of(s1));
        when(feedbackRepository.findBySessionIdInWithSession(List.of(50L)))
                .thenReturn(List.of(f1));

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.guest(testGuestId));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(1);
        assertThat(result.getSummary().getTotalSpeakingTimeSeconds()).isEqualTo(175);
        assertThat(result.getSummary().getAverageOverallScore()).isEqualTo(92.0);
        assertThat(result.getModeBreakdown().getDebateCount()).isEqualTo(1);
        assertThat(result.getModeBreakdown().getOffTheCuffCount()).isEqualTo(0);
        assertThat(result.getScoreHistory()).hasSize(1);
        assertThat(result.getRecentActivity()).hasSize(1);
    }

    @Test
    @DisplayName("Completed sessions with zero feedback return total counts but null score averages")
    void completedSessions_withZeroFeedback_returnsNullScores() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        Instant now = Instant.now();
        Session s1 = createSession(10L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, now, "No Feedback");

        when(sessionRepository.findByUserAndStatusOrderByChronologicalAsc(testUser, SessionStatus.COMPLETED))
                .thenReturn(List.of(s1));
        when(feedbackRepository.findBySessionIdInWithSession(List.of(10L)))
                .thenReturn(List.of());

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.authenticated(10L));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(1);
        assertThat(result.getSummary().getTotalSpeakingTimeSeconds()).isEqualTo(60);
        assertThat(result.getSummary().getReviewedSessionsCount()).isEqualTo(0);
        assertThat(result.getSummary().getAverageOverallScore()).isNull();
        assertThat(result.getSkills().getAverageClarity()).isNull();
        assertThat(result.getSkills().getAverageRelevance()).isNull();
        assertThat(result.getSkills().getAverageStructure()).isNull();
        assertThat(result.getScoreHistory()).isEmpty();
        assertThat(result.getRecentActivity()).hasSize(1);
        assertThat(result.getRecentActivity().get(0).isHasFeedback()).isFalse();
        assertThat(result.getRecentActivity().get(0).getOverallScore()).isNull();
    }

    @Test
    @DisplayName("Recent activity is limited to maximum 5 items")
    void recentActivity_limitedToFiveItems() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));

        Instant base = Instant.parse("2026-09-01T00:00:00Z");
        List<Session> sevenSessions = List.of(
                createSession(1L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(100), "P1"),
                createSession(2L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(200), "P2"),
                createSession(3L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(300), "P3"),
                createSession(4L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(400), "P4"),
                createSession(5L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(500), "P5"),
                createSession(6L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(600), "P6"),
                createSession(7L, testUser, null, Mode.OFF_THE_CUFF, 60, 60, base.plusSeconds(700), "P7")
        );

        when(sessionRepository.findByUserAndStatusOrderByChronologicalAsc(testUser, SessionStatus.COMPLETED))
                .thenReturn(sevenSessions);
        when(feedbackRepository.findBySessionIdInWithSession(any()))
                .thenReturn(List.of());

        ProgressDashboardDto result = analyticsService.getProgressDashboard(CallerContext.authenticated(10L));

        assertThat(result.getSummary().getTotalCompletedSessions()).isEqualTo(7);
        assertThat(result.getRecentActivity()).hasSize(5);
        // The most recent session is ID 7, followed by 6, 5, 4, 3
        assertThat(result.getRecentActivity().get(0).getSessionId()).isEqualTo(7L);
        assertThat(result.getRecentActivity().get(1).getSessionId()).isEqualTo(6L);
        assertThat(result.getRecentActivity().get(2).getSessionId()).isEqualTo(5L);
        assertThat(result.getRecentActivity().get(3).getSessionId()).isEqualTo(4L);
        assertThat(result.getRecentActivity().get(4).getSessionId()).isEqualTo(3L);
    }

    private Session createSession(Long id, User user, String guestId, Mode mode,
                                  int duration, Integer actualDuration, Instant completedAt, String promptText) {
        Session s = new Session();
        s.setId(id);
        s.setUser(user);
        s.setGuestId(guestId);
        s.setMode(mode);
        s.setStatus(SessionStatus.COMPLETED);
        s.setDurationSeconds(duration);
        s.setActualDurationSeconds(actualDuration);
        s.setStartedAt(completedAt.minusSeconds(duration));
        s.setCompletedAt(completedAt);
        s.setCreatedAt(completedAt.minusSeconds(duration));
        s.setPromptText(promptText);
        return s;
    }

    private Feedback createFeedback(Long id, Session session, int overall, int clarity, int relevance, int structure) {
        Feedback f = new Feedback();
        f.setId(id);
        f.setSession(session);
        f.setOverallScore(overall);
        f.setClarityScore(clarity);
        f.setRelevanceScore(relevance);
        f.setStructureScore(structure);
        f.setSummary("Great delivery");
        return f;
    }
}
