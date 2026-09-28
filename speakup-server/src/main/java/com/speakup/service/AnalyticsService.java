package com.speakup.service;

import com.speakup.dto.*;
import com.speakup.model.Feedback;
import com.speakup.model.Mode;
import com.speakup.model.Session;
import com.speakup.model.SessionStatus;
import com.speakup.model.User;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.CallerContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    private final SessionRepository sessionRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public AnalyticsService(SessionRepository sessionRepository,
                            FeedbackRepository feedbackRepository,
                            UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    /**
     * Compute progress dashboard metrics scoped strictly to the current caller.
     * Authenticated callers retrieve stats for their user ID.
     * Guest callers retrieve stats for their persistent guestId where user IS NULL.
     * Anonymous callers receive empty baseline statistics.
     */
    @Transactional(readOnly = true)
    public ProgressDashboardDto getProgressDashboard(CallerContext caller) {
        if (caller == null || caller.isAnonymous()) {
            return ProgressDashboardDto.empty();
        }

        List<Session> completedSessions;
        if (caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId()).orElse(null);
            if (user == null) {
                return ProgressDashboardDto.empty();
            }
            completedSessions = sessionRepository.findByUserAndStatusOrderByChronologicalAsc(user, SessionStatus.COMPLETED);
        } else if (caller.isGuest()) {
            completedSessions = sessionRepository.findByGuestIdAndUserIsNullAndStatusOrderByChronologicalAsc(
                    caller.getGuestId(), SessionStatus.COMPLETED);
        } else {
            return ProgressDashboardDto.empty();
        }

        if (completedSessions.isEmpty()) {
            return ProgressDashboardDto.empty();
        }

        List<Long> sessionIds = completedSessions.stream().map(Session::getId).toList();
        List<Feedback> feedbacks = sessionIds.isEmpty()
                ? List.of()
                : feedbackRepository.findBySessionIdInWithSession(sessionIds);

        Map<Long, Feedback> feedbackBySessionId = feedbacks.stream()
                .collect(Collectors.toMap(f -> f.getSession().getId(), f -> f, (existing, replacement) -> existing));

        // 1. Summary
        int totalCompleted = completedSessions.size();
        int totalSpeakingTime = completedSessions.stream()
                .mapToInt(s -> s.getActualDurationSeconds() != null ? s.getActualDurationSeconds() : s.getDurationSeconds())
                .sum();
        int reviewedCount = feedbacks.size();
        Double avgOverall = feedbacks.isEmpty()
                ? null
                : round(feedbacks.stream().mapToInt(Feedback::getOverallScore).average().orElse(0.0));

        ProgressSummaryDto summary = new ProgressSummaryDto(totalCompleted, totalSpeakingTime, avgOverall, reviewedCount);

        // 2. Skills
        Double avgClarity = feedbacks.isEmpty()
                ? null
                : round(feedbacks.stream().mapToInt(Feedback::getClarityScore).average().orElse(0.0));
        Double avgRelevance = feedbacks.isEmpty()
                ? null
                : round(feedbacks.stream().mapToInt(Feedback::getRelevanceScore).average().orElse(0.0));
        Double avgStructure = feedbacks.isEmpty()
                ? null
                : round(feedbacks.stream().mapToInt(Feedback::getStructureScore).average().orElse(0.0));

        SkillMetricsDto skills = new SkillMetricsDto(avgClarity, avgRelevance, avgStructure);

        // 3. Mode Breakdown
        int offTheCuff = 0;
        int research = 0;
        int debate = 0;
        int story = 0;
        for (Session s : completedSessions) {
            if (s.getMode() == Mode.OFF_THE_CUFF) {
                offTheCuff++;
            } else if (s.getMode() == Mode.RESEARCH) {
                research++;
            } else if (s.getMode() == Mode.DEBATE) {
                debate++;
            } else if (s.getMode() == Mode.STORY) {
                story++;
            }
        }
        ModeBreakdownDto modeBreakdown = new ModeBreakdownDto(offTheCuff, research, debate, story);

        // 4. Score History (Chronological ASC, only sessions with feedback)
        List<ScoreHistoryPointDto> scoreHistory = new ArrayList<>();
        for (Session s : completedSessions) {
            Feedback f = feedbackBySessionId.get(s.getId());
            if (f != null && f.getOverallScore() != null) {
                Instant date = s.getCompletedAt() != null ? s.getCompletedAt() : s.getCreatedAt();
                scoreHistory.add(new ScoreHistoryPointDto(
                        s.getId(),
                        date,
                        s.getMode().name(),
                        f.getOverallScore(),
                        f.getClarityScore(),
                        f.getRelevanceScore(),
                        f.getStructureScore(),
                        s.getPromptText()
                ));
            }
        }

        // 5. Recent Activity (Recent 5 completed sessions DESC)
        List<RecentSessionActivityDto> recentActivity = completedSessions.stream()
                .sorted(Comparator.comparing((Session s) -> s.getCompletedAt() != null ? s.getCompletedAt() : s.getCreatedAt())
                        .thenComparing(Session::getId)
                        .reversed())
                .limit(5)
                .map(s -> {
                    Feedback f = feedbackBySessionId.get(s.getId());
                    int duration = s.getActualDurationSeconds() != null ? s.getActualDurationSeconds() : s.getDurationSeconds();
                    Instant completedAt = s.getCompletedAt() != null ? s.getCompletedAt() : s.getCreatedAt();
                    return new RecentSessionActivityDto(
                            s.getId(),
                            s.getPromptText(),
                            s.getMode().name(),
                            completedAt,
                            duration,
                            f != null ? f.getOverallScore() : null,
                            f != null
                    );
                })
                .toList();

        return new ProgressDashboardDto(summary, skills, modeBreakdown, scoreHistory, recentActivity);
    }

    private Double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
