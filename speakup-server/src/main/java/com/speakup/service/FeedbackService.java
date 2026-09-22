package com.speakup.service;

import com.speakup.dto.FeedbackDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.FeedbackMapper;
import com.speakup.model.Feedback;
import com.speakup.model.Session;
import com.speakup.model.SessionStatus;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.service.ai.AiFeedbackResponse;
import com.speakup.service.ai.AiSpeakingCoachClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FeedbackService {

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final int MIN_TRANSCRIPT_WORDS = 5;

    private final FeedbackRepository feedbackRepository;
    private final SessionRepository sessionRepository;
    private final AiSpeakingCoachClient aiSpeakingCoachClient;

    public FeedbackService(
            FeedbackRepository feedbackRepository,
            SessionRepository sessionRepository,
            AiSpeakingCoachClient aiSpeakingCoachClient) {
        this.feedbackRepository = feedbackRepository;
        this.sessionRepository = sessionRepository;
        this.aiSpeakingCoachClient = aiSpeakingCoachClient;
    }

    /**
     * Generate or retrieve feedback for a completed session.
     * Idempotent: If feedback already exists for the session, returns the persisted feedback
     * without calling the AI service again.
     */
    @Transactional
    public FeedbackDto generateOrGetFeedback(Long sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + sessionId));

        // Return existing feedback if already generated
        Optional<Feedback> existing = feedbackRepository.findBySessionId(sessionId);
        if (existing.isPresent()) {
            log.info("Returning cached feedback for session id: {}", sessionId);
            return FeedbackMapper.toDto(existing.get());
        }

        // Validate session is completed
        if (session.getStatus() != SessionStatus.COMPLETED) {
            throw new IllegalArgumentException("Feedback can only be generated for completed sessions");
        }

        // Validate transcript existence and length
        String transcript = session.getTranscript();
        if (transcript == null || transcript.trim().isEmpty()) {
            throw new IllegalArgumentException("Cannot generate feedback for an empty transcript");
        }

        int wordCount = transcript.trim().split("\\s+").length;
        if (wordCount < MIN_TRANSCRIPT_WORDS) {
            throw new IllegalArgumentException(
                    "Transcript is too short to generate feedback (minimum " + MIN_TRANSCRIPT_WORDS + " words required)"
            );
        }

        String categoryName = (session.getPrompt() != null && session.getPrompt().getCategory() != null)
                ? session.getPrompt().getCategory().getName()
                : "General";

        int actualDuration = (session.getActualDurationSeconds() != null)
                ? session.getActualDurationSeconds()
                : session.getDurationSeconds();

        String stanceName = session.getStance() != null ? session.getStance().name() : null;

        // Call AI speaking coach client
        AiFeedbackResponse aiResponse = aiSpeakingCoachClient.analyzeSpeaking(
                session.getMode().name(),
                session.getPromptText(),
                categoryName,
                session.getDurationSeconds(),
                actualDuration,
                transcript,
                session.getPreparationNotes(),
                stanceName
        );

        // Build and persist Feedback entity
        Feedback feedback = new Feedback();
        feedback.setSession(session);
        feedback.setOverallScore(aiResponse.getOverallScore());
        feedback.setClarityScore(aiResponse.getClarityScore());
        feedback.setRelevanceScore(aiResponse.getRelevanceScore());
        feedback.setStructureScore(aiResponse.getStructureScore());
        feedback.setSummary(aiResponse.getSummary());
        feedback.setStrengths(FeedbackMapper.serializeList(aiResponse.getStrengths()));
        feedback.setImprovements(FeedbackMapper.serializeList(aiResponse.getImprovements()));

        Feedback saved = feedbackRepository.save(feedback);
        log.info("Persisted new AI feedback for session id: {}", sessionId);

        return FeedbackMapper.toDto(saved);
    }

    /**
     * Retrieve already persisted feedback for a session.
     */
    @Transactional(readOnly = true)
    public FeedbackDto getFeedbackBySessionId(Long sessionId) {
        if (!sessionRepository.existsById(sessionId)) {
            throw new ResourceNotFoundException("Session not found with id: " + sessionId);
        }

        Feedback feedback = feedbackRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("No feedback found for session id: " + sessionId));

        return FeedbackMapper.toDto(feedback);
    }
}
