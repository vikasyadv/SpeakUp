package com.speakup.service;

import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.dto.SessionDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.SessionMapper;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.CallerContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;
    private final PromptRepository promptRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    public SessionService(SessionRepository sessionRepository,
                          PromptRepository promptRepository,
                          FeedbackRepository feedbackRepository,
                          UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.promptRepository = promptRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    /**
     * Create a new session when the user starts speaking with caller ownership.
     */
    @Transactional
    public SessionDto createSession(SessionCreateDto dto, CallerContext caller) {
        Session session = new Session();
        session.setPromptText(dto.getPromptText());
        session.setMode(Mode.valueOf(dto.getMode().toUpperCase()));
        session.setDurationSeconds(dto.getDurationSeconds());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());

        // Assign exclusive ownership based on CallerContext
        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + caller.getUserId()));
            session.setUser(user);
            session.setGuestId(null);
        } else if (caller != null && caller.isGuest()) {
            session.setUser(null);
            session.setGuestId(caller.getGuestId());
        } else {
            session.setUser(null);
            session.setGuestId(null);
        }

        // Link to prompt entity if ID provided
        if (dto.getPromptId() != null) {
            Prompt prompt = promptRepository.findById(dto.getPromptId()).orElse(null);
            session.setPrompt(prompt);
        }

        if (dto.getStance() != null && !dto.getStance().isBlank()) {
            session.setStance(Stance.fromString(dto.getStance()));
        }

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    public SessionDto createSession(SessionCreateDto dto) {
        return createSession(dto, CallerContext.anonymous());
    }

    /**
     * Mark a session as completed with caller ownership check.
     */
    @Transactional
    public SessionDto completeSession(Long id, SessionCompleteDto dto, CallerContext caller) {
        Session session = getSessionAndVerifyOwnership(id, caller);

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
            if (dto.getPreparationNotes() != null) {
                session.setPreparationNotes(dto.getPreparationNotes().trim());
            }
            if (dto.getPreparationDurationSeconds() != null) {
                session.setPreparationDurationSeconds(dto.getPreparationDurationSeconds());
            }
            if (dto.getStance() != null && !dto.getStance().isBlank()) {
                session.setStance(Stance.fromString(dto.getStance()));
            }
        } else {
            session.setActualDurationSeconds(session.getDurationSeconds());
        }

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    public SessionDto completeSession(Long id, SessionCompleteDto dto) {
        return completeSession(id, dto, CallerContext.anonymous());
    }

    public SessionDto completeSession(Long id) {
        return completeSession(id, null, CallerContext.anonymous());
    }

    /**
     * Mark a session as abandoned with caller ownership check.
     */
    @Transactional
    public SessionDto abandonSession(Long id, CallerContext caller) {
        Session session = getSessionAndVerifyOwnership(id, caller);

        session.setStatus(SessionStatus.ABANDONED);
        session.setCompletedAt(Instant.now());

        Session saved = sessionRepository.save(session);
        return SessionMapper.toDto(saved);
    }

    public SessionDto abandonSession(Long id) {
        return abandonSession(id, CallerContext.anonymous());
    }

    /**
     * Get a single session by ID with caller ownership check.
     */
    public SessionDto getById(Long id, CallerContext caller) {
        Session session = getSessionAndVerifyOwnership(id, caller);
        boolean hasFeedback = feedbackRepository.existsBySessionId(id);
        return SessionMapper.toDto(session, hasFeedback);
    }

    public SessionDto getById(Long id) {
        return getById(id, CallerContext.anonymous());
    }

    /**
     * Get recent session history scoped to current caller (last 20).
     */
    public List<SessionDto> getRecentSessions(CallerContext caller) {
        List<Session> sessions;

        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId()).orElse(null);
            if (user == null) {
                return List.of();
            }
            sessions = sessionRepository.findTop20ByUserOrderByCreatedAtDesc(user);
        } else if (caller != null && caller.isGuest()) {
            sessions = sessionRepository.findTop20ByGuestIdAndUserIsNullOrderByCreatedAtDesc(caller.getGuestId());
        } else {
            sessions = sessionRepository.findTop20ByOrderByCreatedAtDesc();
        }

        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream().map(Session::getId).toList();
        Set<Long> feedbackSessionIds = feedbackRepository.findSessionIdsWithFeedback(sessionIds);

        return sessions.stream()
                .map(s -> SessionMapper.toDto(s, feedbackSessionIds != null && feedbackSessionIds.contains(s.getId())))
                .toList();
    }

    public List<SessionDto> getRecentSessions() {
        return getRecentSessions(CallerContext.anonymous());
    }

    /**
     * Get paginated sessions with optional mode, status, and keyword search filtering,
     * strictly scoped to current caller.
     */
    public Page<SessionDto> getSessions(
            String modeStr,
            String statusStr,
            String search,
            Pageable pageable,
            CallerContext caller
    ) {
        Mode mode = parseMode(modeStr);
        SessionStatus status = parseStatus(statusStr);
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;

        Pageable effectivePageable = resolveDeterministicPageable(pageable);
        String searchPattern = cleanSearch != null ? "%" + cleanSearch.toLowerCase() + "%" : null;

        Page<Session> sessionPage;
        if (caller != null && caller.isAuthenticated()) {
            User user = userRepository.findById(caller.getUserId()).orElse(null);
            if (user == null) {
                return Page.empty(effectivePageable);
            }
            sessionPage = sessionRepository.findByUserWithFilters(user, mode, status, searchPattern, effectivePageable);
        } else if (caller != null && caller.isGuest()) {
            sessionPage = sessionRepository.findByGuestWithFilters(caller.getGuestId(), mode, status, searchPattern, effectivePageable);
        } else {
            sessionPage = sessionRepository.findAnonymousWithFilters(mode, status, searchPattern, effectivePageable);
        }

        if (sessionPage.isEmpty()) {
            return sessionPage.map(s -> SessionMapper.toDto(s, false));
        }

        List<Long> sessionIds = sessionPage.getContent().stream().map(Session::getId).toList();
        Set<Long> feedbackSessionIds = feedbackRepository.findSessionIdsWithFeedback(sessionIds);

        return sessionPage.map(s -> SessionMapper.toDto(s, feedbackSessionIds != null && feedbackSessionIds.contains(s.getId())));
    }

    public Page<SessionDto> getSessions(String modeStr, String statusStr, String search, Pageable pageable) {
        return getSessions(modeStr, statusStr, search, pageable, CallerContext.anonymous());
    }

    private Mode parseMode(String modeStr) {
        if (modeStr == null || modeStr.isBlank()) {
            return null;
        }
        String normalized = modeStr.trim().toUpperCase().replace('-', '_');
        try {
            return Mode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid mode: '" + modeStr + "'. Supported modes are: " + Arrays.toString(Mode.values()));
        }
    }

    private SessionStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return null;
        }
        String normalized = statusStr.trim().toUpperCase().replace('-', '_');
        try {
            return SessionStatus.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: '" + statusStr + "'. Supported statuses are: " + Arrays.toString(SessionStatus.values()));
        }
    }

    private Pageable resolveDeterministicPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        }
        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt", "id")
            );
        }
        boolean hasIdSort = false;
        for (Sort.Order order : pageable.getSort()) {
            if ("id".equalsIgnoreCase(order.getProperty())) {
                hasIdSort = true;
                break;
            }
        }
        if (!hasIdSort) {
            return PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    pageable.getSort().and(Sort.by(Sort.Direction.DESC, "id"))
            );
        }
        return pageable;
    }

    /**
     * Delete a session with caller ownership check.
     */
    @Transactional
    public void deleteSession(Long id, CallerContext caller) {
        if (caller == null || caller.isAnonymous()) {
            if (!sessionRepository.existsById(id)) {
                throw new ResourceNotFoundException("Session not found with id: " + id);
            }
            sessionRepository.deleteById(id);
            return;
        }

        Session session = getSessionAndVerifyOwnership(id, caller);
        sessionRepository.delete(session);
    }

    public void deleteSession(Long id) {
        deleteSession(id, CallerContext.anonymous());
    }

    /**
     * Retrieve session and verify that caller has ownership.
     * Returns 404 if not found OR if ownership does not match, to prevent IDOR existence probing.
     */
    public Session getSessionAndVerifyOwnership(Long id, CallerContext caller) {
        Session session = sessionRepository.findByIdWithPrompt(id)
                .or(() -> sessionRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found with id: " + id));

        if (!isOwnedBy(session, caller)) {
            throw new ResourceNotFoundException("Session not found with id: " + id);
        }
        return session;
    }

    /**
     * Check if a session is owned by the given caller context.
     */
    public static boolean isOwnedBy(Session session, CallerContext caller) {
        if (session == null || caller == null) {
            return false;
        }

        if (caller.isAuthenticated()) {
            return session.getUser() != null && caller.getUserId().equals(session.getUser().getId());
        }

        if (caller.isGuest()) {
            return session.getUser() == null && caller.getGuestId().equals(session.getGuestId());
        }

        // Anonymous callers only match legacy unowned records where both user and guestId are null
        if (caller.isAnonymous()) {
            return session.getUser() == null && session.getGuestId() == null;
        }

        return false;
    }
}
