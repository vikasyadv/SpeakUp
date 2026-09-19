package com.speakup.service;

import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.dto.SessionDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private SessionService sessionService;

    private Session sampleSession;
    private Prompt samplePrompt;
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

        sampleSession = new Session();
        sampleSession.setId(1L);
        sampleSession.setPromptText("Artificial Intelligence");
        sampleSession.setPrompt(samplePrompt);
        sampleSession.setMode(Mode.OFF_THE_CUFF);
        sampleSession.setDurationSeconds(60);
        sampleSession.setStatus(SessionStatus.IN_PROGRESS);
        sampleSession.setStartedAt(Instant.now());
        sampleSession.setCreatedAt(Instant.now());
    }

    @Test
    void createSession_createsAndReturnsSession() {
        SessionCreateDto dto = new SessionCreateDto("Artificial Intelligence", 1L, "OFF_THE_CUFF", 60);
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));
        when(sessionRepository.save(any(Session.class))).thenReturn(sampleSession);

        SessionDto result = sessionService.createSession(dto);

        assertNotNull(result);
        assertEquals("Artificial Intelligence", result.getPromptText());
        assertEquals("OFF_THE_CUFF", result.getMode());
        assertEquals(60, result.getDurationSeconds());
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void createSession_worksWithoutPromptId() {
        SessionCreateDto dto = new SessionCreateDto("Custom Topic", null, "OFF_THE_CUFF", 30);
        Session sessionWithoutPrompt = new Session();
        sessionWithoutPrompt.setId(2L);
        sessionWithoutPrompt.setPromptText("Custom Topic");
        sessionWithoutPrompt.setMode(Mode.OFF_THE_CUFF);
        sessionWithoutPrompt.setDurationSeconds(30);
        sessionWithoutPrompt.setStatus(SessionStatus.IN_PROGRESS);
        sessionWithoutPrompt.setStartedAt(Instant.now());
        sessionWithoutPrompt.setCreatedAt(Instant.now());

        when(sessionRepository.save(any(Session.class))).thenReturn(sessionWithoutPrompt);

        SessionDto result = sessionService.createSession(dto);

        assertNotNull(result);
        assertEquals("Custom Topic", result.getPromptText());
        assertNull(result.getPromptId());
        verify(promptRepository, never()).findById(any());
    }

    @Test
    void completeSession_updatesStatusToCompleted() {
        Session completedSession = new Session();
        completedSession.setId(1L);
        completedSession.setPromptText("Artificial Intelligence");
        completedSession.setMode(Mode.OFF_THE_CUFF);
        completedSession.setDurationSeconds(60);
        completedSession.setActualDurationSeconds(60);
        completedSession.setStatus(SessionStatus.COMPLETED);
        completedSession.setStartedAt(Instant.now());
        completedSession.setCompletedAt(Instant.now());
        completedSession.setCreatedAt(Instant.now());

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(completedSession);

        SessionDto result = sessionService.completeSession(1L);

        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(60, result.getActualDurationSeconds());
    }

    @Test
    void completeSession_withTranscriptAndActualDuration_savesAndReturnsBoth() {
        Session completedSession = new Session();
        completedSession.setId(1L);
        completedSession.setPromptText("Artificial Intelligence");
        completedSession.setMode(Mode.OFF_THE_CUFF);
        completedSession.setDurationSeconds(60);
        completedSession.setActualDurationSeconds(42);
        completedSession.setTranscript("Artificial intelligence is transforming industries.");
        completedSession.setStatus(SessionStatus.COMPLETED);
        completedSession.setStartedAt(Instant.now());
        completedSession.setCompletedAt(Instant.now());
        completedSession.setCreatedAt(Instant.now());

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(completedSession);

        SessionCompleteDto dto = new SessionCompleteDto("  Artificial intelligence is transforming industries.  ", 42);
        SessionDto result = sessionService.completeSession(1L, dto);

        assertEquals("COMPLETED", result.getStatus());
        assertNotNull(result.getCompletedAt());
        assertEquals(42, result.getActualDurationSeconds());
        assertEquals("Artificial intelligence is transforming industries.", result.getTranscript());
    }

    @Test
    void completeSession_withPreparationNotesAndDuration_savesAndReturnsBoth() {
        Session completedSession = new Session();
        completedSession.setId(1L);
        completedSession.setPromptText("How will AI transform healthcare?");
        completedSession.setMode(Mode.RESEARCH);
        completedSession.setDurationSeconds(120);
        completedSession.setActualDurationSeconds(115);
        completedSession.setTranscript("AI enables earlier diagnosis and personalized medicine.");
        completedSession.setPreparationNotes("Key points: 1. Diagnostics 2. Personalized therapy");
        completedSession.setPreparationDurationSeconds(300);
        completedSession.setStatus(SessionStatus.COMPLETED);
        completedSession.setStartedAt(Instant.now());
        completedSession.setCompletedAt(Instant.now());
        completedSession.setCreatedAt(Instant.now());

        when(sessionRepository.findById(1L)).thenReturn(Optional.of(sampleSession));
        when(sessionRepository.save(any(Session.class))).thenReturn(completedSession);

        SessionCompleteDto dto = new SessionCompleteDto(
                "  AI enables earlier diagnosis and personalized medicine.  ",
                115,
                "  Key points: 1. Diagnostics 2. Personalized therapy  ",
                300
        );
        SessionDto result = sessionService.completeSession(1L, dto);

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(115, result.getActualDurationSeconds());
        assertEquals("AI enables earlier diagnosis and personalized medicine.", result.getTranscript());
        assertEquals("Key points: 1. Diagnostics 2. Personalized therapy", result.getPreparationNotes());
        assertEquals(300, result.getPreparationDurationSeconds());
    }

    @Test
    void completeSession_throwsWhenNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sessionService.completeSession(99L));
    }

    @Test
    void getById_returnsSession() {
        when(sessionRepository.findByIdWithPrompt(1L)).thenReturn(Optional.of(sampleSession));
        when(feedbackRepository.existsBySessionId(1L)).thenReturn(true);

        SessionDto result = sessionService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Artificial Intelligence", result.getPromptText());
        assertEquals("Technology", result.getCategory());
        assertTrue(result.getHasFeedback());
    }

    @Test
    void getById_throwsWhenNotFound() {
        when(sessionRepository.findByIdWithPrompt(99L)).thenReturn(Optional.empty());
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> sessionService.getById(99L));
    }

    @Test
    void getRecentSessions_returnsList() {
        when(sessionRepository.findTop20ByOrderByCreatedAtDesc())
                .thenReturn(List.of(sampleSession));
        when(feedbackRepository.findSessionIdsWithFeedback(List.of(1L)))
                .thenReturn(Set.of(1L));

        List<SessionDto> result = sessionService.getRecentSessions();

        assertEquals(1, result.size());
        assertEquals("Artificial Intelligence", result.get(0).getPromptText());
        assertEquals("Technology", result.get(0).getCategory());
        assertTrue(result.get(0).getHasFeedback());
    }

    @Test
    void deleteSession_deletesWhenExists() {
        when(sessionRepository.existsById(1L)).thenReturn(true);

        sessionService.deleteSession(1L);

        verify(sessionRepository).deleteById(1L);
    }

    @Test
    void deleteSession_throwsWhenNotFound() {
        when(sessionRepository.existsById(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class,
                () -> sessionService.deleteSession(99L));
    }
}
