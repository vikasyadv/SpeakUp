package com.speakup.service;

import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.dto.SessionDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.SessionMapper;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.model.Session;
import com.speakup.model.SessionStatus;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final PromptRepository promptRepository;
    private final FeedbackRepository feedbackRepository;

    public SessionService(SessionRepository sessionRepository,
                          PromptRepository promptRepository,
                          FeedbackRepository feedbackRepository) {
        this.sessionRepository = sessionRepository;
        this.promptRepository = promptRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * Create a new session when the user starts speaking.
     */
    @Transactional
    public SessionDto createSession(SessionCreateDto dto) {
        Session session = new Session();
        session.setPromptText(dto.getPromptText());
        session.setMode(Mode.valueOf(dto.getMode().toUpperCase()));
        session.setDurationSeconds(dto.getDurationSeconds());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());

        // Link to prompt entity if ID provided
        if (dto.getPromptId() != null) {
            Prompt prompt = promptRepository.findById(dto.getPromptId()).orElse(null);
            session.setPrompt(prompt);
        }

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    /**
     * Mark a session as completed.
     */
    @Transactional
    public SessionDto completeSession(Long id) {
        return completeSession(id, null);
    }

    /**
     * Mark a session as completed with optional transcript and actual duration.
     */
    @Transactional
    public SessionDto completeSession(Long id, SessionCompleteDto dto) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        session.setStatus(SessionStatus.COMPLETED);
        session.setCompletedAt(Instant.now());

        if (dto != null) {
            if (dto.getTranscript() != null) {
                session.setTranscript(dto.getTranscript().trim());
            }
            if (dto.getActualDurationSeconds() != null) {
                session.setActualDurationSeconds(dto.getActualDurationSeconds());
            } else {
                session.setActualDurationSeconds(session.getDurationSeconds());
            }
        } else {
            session.setActualDurationSeconds(session.getDurationSeconds());
        }

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    /**
     * Mark a session as abandoned (user navigated away without finishing).
     */
    @Transactional
    public SessionDto abandonSession(Long id) {
        Session session = sessionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        session.setStatus(SessionStatus.ABANDONED);
        session.setCompletedAt(Instant.now());

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    /**
     * Get a single session by ID.
     */
    public SessionDto getById(Long id) {
        Session session = sessionRepository.findByIdWithPrompt(id)
                .or(() -> sessionRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));
        boolean hasFeedback = feedbackRepository.existsBySessionId(id);
        return SessionMapper.toDto(session, hasFeedback);
    }

    /**
     * Get recent session history (last 20).
     */
    public List<SessionDto> getRecentSessions() {
        List<Session> sessions = sessionRepository.findTop20ByOrderByCreatedAtDesc();
        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        Set<Long> feedbackSessionIds = feedbackRepository.findSessionIdsWithFeedback(sessionIds);

        return sessions.stream()
                .map(s -> SessionMapper.toDto(s, feedbackSessionIds != null && feedbackSessionIds.contains(s.getId())))
                .toList();
    }

    /**
     * Delete a session.
     */
    @Transactional
    public void deleteSession(Long id) {
        if (!sessionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Session not found with id: " + id);
        }
        sessionRepository.deleteById(id);
    }
}
