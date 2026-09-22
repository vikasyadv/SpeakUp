package com.speakup.service;

import com.speakup.dto.FeedbackDto;
import com.speakup.exception.AiServiceException;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.service.ai.AiFeedbackResponse;
import com.speakup.service.ai.AiSpeakingCoachClient;
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
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private AiSpeakingCoachClient aiSpeakingCoachClient;

    @InjectMocks
    private FeedbackService feedbackService;

    private Session completedSession;
    private Prompt samplePrompt;
    private Category sampleCategory;
    private AiFeedbackResponse sampleAiResponse;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category("Technology", "Tech topics");
        sampleCategory.setId(1L);

        samplePrompt = new Prompt();
        samplePrompt.setId(1L);
        samplePrompt.setText("Artificial Intelligence");
        samplePrompt.setCategory(sampleCategory);
        samplePrompt.setMode(Mode.OFF_THE_CUFF);

        completedSession = new Session();
        completedSession.setId(10L);
        completedSession.setPrompt(samplePrompt);
        completedSession.setPromptText("Artificial Intelligence");
        completedSession.setMode(Mode.OFF_THE_CUFF);
        completedSession.setDurationSeconds(60);
        completedSession.setActualDurationSeconds(45);
        completedSession.setStatus(SessionStatus.COMPLETED);
        completedSession.setTranscript("Artificial intelligence is rapidly transforming various industries across the modern world today.");
        completedSession.setStartedAt(Instant.now());
        completedSession.setCompletedAt(Instant.now());

        sampleAiResponse = new AiFeedbackResponse(
                8,
                9,
                8,
                7,
                "Strong clear speech with direct relevance to the topic.",
                List.of("Direct answer", "Clear vocabulary"),
                List.of("Add a concrete example", "Structure a stronger conclusion")
        );
    }

    @Test
    void generateOrGetFeedback_completedSessionWithValidTranscript_callsAiAndPersistsFeedback() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());
        when(aiSpeakingCoachClient.analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .thenReturn(sampleAiResponse);

        Feedback savedEntity = new Feedback();
        savedEntity.setId(100L);
        savedEntity.setSession(completedSession);
        savedEntity.setOverallScore(8);
        savedEntity.setClarityScore(9);
        savedEntity.setRelevanceScore(8);
        savedEntity.setStructureScore(7);
        savedEntity.setSummary("Strong clear speech with direct relevance to the topic.");
        savedEntity.setStrengths("[\"Direct answer\",\"Clear vocabulary\"]");
        savedEntity.setImprovements("[\"Add a concrete example\",\"Structure a stronger conclusion\"]");
        savedEntity.setCreatedAt(Instant.now());

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedEntity);

        FeedbackDto result = feedbackService.generateOrGetFeedback(10L);

        assertNotNull(result);
        assertEquals(8, result.getOverallScore());
        assertEquals(9, result.getClarityScore());
        assertEquals(8, result.getRelevanceScore());
        assertEquals(7, result.getStructureScore());
        assertEquals(2, result.getStrengths().size());
        assertEquals(2, result.getImprovements().size());

        verify(aiSpeakingCoachClient, times(1)).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
        verify(feedbackRepository, times(1)).save(any(Feedback.class));
    }

    @Test
    void generateOrGetFeedback_missingSession_throwsResourceNotFound() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> feedbackService.generateOrGetFeedback(99L));
        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void generateOrGetFeedback_sessionNotCompleted_throwsIllegalArgument() {
        completedSession.setStatus(SessionStatus.IN_PROGRESS);
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));

        assertThrows(IllegalArgumentException.class, () -> feedbackService.generateOrGetFeedback(10L));
        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void generateOrGetFeedback_emptyTranscript_throwsIllegalArgument() {
        completedSession.setTranscript("   ");
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));

        assertThrows(IllegalArgumentException.class, () -> feedbackService.generateOrGetFeedback(10L));
        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void generateOrGetFeedback_transcriptTooShort_throwsIllegalArgument() {
        completedSession.setTranscript("Too short transcript"); // only 3 words
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));

        assertThrows(IllegalArgumentException.class, () -> feedbackService.generateOrGetFeedback(10L));
        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
    }

    @Test
    void generateOrGetFeedback_aiThrowsException_propagatesException() {
        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());
        when(aiSpeakingCoachClient.analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any()))
                .thenThrow(new AiServiceException("Gemini quota exceeded"));

        assertThrows(AiServiceException.class, () -> feedbackService.generateOrGetFeedback(10L));
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void generateOrGetFeedback_alreadyGenerated_returnsCachedWithoutAiCall() {
        Feedback existing = new Feedback();
        existing.setId(99L);
        existing.setSession(completedSession);
        existing.setOverallScore(9);
        existing.setClarityScore(9);
        existing.setRelevanceScore(9);
        existing.setStructureScore(9);
        existing.setSummary("Cached assessment");
        existing.setStrengths("[\"Great job\"]");
        existing.setImprovements("[\"Keep practicing\"]");
        existing.setCreatedAt(Instant.now());

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.of(existing));

        FeedbackDto result = feedbackService.generateOrGetFeedback(10L);

        assertNotNull(result);
        assertEquals(9, result.getOverallScore());
        assertEquals("Cached assessment", result.getSummary());

        // ZERO calls to AI client
        verify(aiSpeakingCoachClient, never()).analyzeSpeaking(any(), any(), any(), anyInt(), anyInt(), any(), any(), any());
        verify(feedbackRepository, never()).save(any());
    }

    @Test
    void getFeedbackBySessionId_returnsPersistedFeedback() {
        Feedback existing = new Feedback();
        existing.setId(50L);
        existing.setSession(completedSession);
        existing.setOverallScore(8);
        existing.setClarityScore(8);
        existing.setRelevanceScore(8);
        existing.setStructureScore(8);
        existing.setSummary("Persisted feedback");
        existing.setStrengths("[\"Good\"]");
        existing.setImprovements("[\"Better\"]");

        when(sessionRepository.existsById(10L)).thenReturn(true);
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.of(existing));

        FeedbackDto result = feedbackService.getFeedbackBySessionId(10L);

        assertNotNull(result);
        assertEquals(8, result.getOverallScore());
        assertEquals("Persisted feedback", result.getSummary());
    }

    @Test
    void getFeedbackBySessionId_notFound_throwsResourceNotFound() {
        when(sessionRepository.existsById(10L)).thenReturn(true);
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> feedbackService.getFeedbackBySessionId(10L));
    }

    @Test
    void generateOrGetFeedback_researchSessionWithPreparationNotes_passesNotesToAi() {
        completedSession.setMode(Mode.RESEARCH);
        completedSession.setPreparationNotes("1. Main thesis on AI in medicine.\n2. Evidence from radiological diagnostics.");
        completedSession.setPreparationDurationSeconds(180);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());
        when(aiSpeakingCoachClient.analyzeSpeaking(
                eq("RESEARCH"),
                eq("Artificial Intelligence"),
                eq("Technology"),
                eq(60),
                eq(45),
                eq(completedSession.getTranscript()),
                eq("1. Main thesis on AI in medicine.\n2. Evidence from radiological diagnostics."),
                isNull()
        )).thenReturn(sampleAiResponse);

        Feedback savedEntity = new Feedback();
        savedEntity.setId(101L);
        savedEntity.setSession(completedSession);
        savedEntity.setOverallScore(8);
        savedEntity.setClarityScore(9);
        savedEntity.setRelevanceScore(8);
        savedEntity.setStructureScore(7);
        savedEntity.setSummary("Strong clear speech with direct relevance to the topic.");
        savedEntity.setStrengths("[\"Direct answer\",\"Clear vocabulary\"]");
        savedEntity.setImprovements("[\"Add a concrete example\",\"Structure a stronger conclusion\"]");
        savedEntity.setCreatedAt(Instant.now());

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedEntity);

        FeedbackDto result = feedbackService.generateOrGetFeedback(10L);

        assertNotNull(result);
        verify(aiSpeakingCoachClient).analyzeSpeaking(
                eq("RESEARCH"),
                eq("Artificial Intelligence"),
                eq("Technology"),
                eq(60),
                eq(45),
                anyString(),
                eq("1. Main thesis on AI in medicine.\n2. Evidence from radiological diagnostics."),
                isNull()
        );
    }

    @Test
    void generateOrGetFeedback_debateSessionWithForStance_passesDebateContextAndStanceToAi() {
        completedSession.setMode(Mode.DEBATE);
        completedSession.setPromptText("Social media platforms do more harm than good.");
        completedSession.setStance(Stance.FOR);
        completedSession.setPreparationNotes("Evidence on teenage mental health and screen time.");

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());
        when(aiSpeakingCoachClient.analyzeSpeaking(
                eq("DEBATE"),
                eq("Social media platforms do more harm than good."),
                eq("Technology"),
                eq(60),
                eq(45),
                eq(completedSession.getTranscript()),
                eq("Evidence on teenage mental health and screen time."),
                eq("FOR")
        )).thenReturn(sampleAiResponse);

        Feedback savedEntity = new Feedback();
        savedEntity.setId(102L);
        savedEntity.setSession(completedSession);
        savedEntity.setOverallScore(8);
        savedEntity.setClarityScore(9);
        savedEntity.setRelevanceScore(8);
        savedEntity.setStructureScore(7);
        savedEntity.setSummary("Persuasive defense of the resolution.");
        savedEntity.setStrengths("[\"Clear premise\",\"Strong rebuttal\"]");
        savedEntity.setImprovements("[\"Cite specific study data\"]");
        savedEntity.setCreatedAt(Instant.now());

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedEntity);

        FeedbackDto result = feedbackService.generateOrGetFeedback(10L);

        assertNotNull(result);
        assertEquals(8, result.getOverallScore());
        verify(aiSpeakingCoachClient).analyzeSpeaking(
                eq("DEBATE"),
                eq("Social media platforms do more harm than good."),
                eq("Technology"),
                eq(60),
                eq(45),
                anyString(),
                eq("Evidence on teenage mental health and screen time."),
                eq("FOR")
        );
    }

    @Test
    void generateOrGetFeedback_debateSessionWithAgainstStance_passesDebateContextAndStanceToAi() {
        completedSession.setMode(Mode.DEBATE);
        completedSession.setPromptText("Nuclear energy is indispensable for net-zero targets.");
        completedSession.setStance(Stance.AGAINST);
        completedSession.setPreparationNotes(null);

        when(sessionRepository.findById(10L)).thenReturn(Optional.of(completedSession));
        when(feedbackRepository.findBySessionId(10L)).thenReturn(Optional.empty());
        when(aiSpeakingCoachClient.analyzeSpeaking(
                eq("DEBATE"),
                eq("Nuclear energy is indispensable for net-zero targets."),
                eq("Technology"),
                eq(60),
                eq(45),
                eq(completedSession.getTranscript()),
                isNull(),
                eq("AGAINST")
        )).thenReturn(sampleAiResponse);

        Feedback savedEntity = new Feedback();
        savedEntity.setId(103L);
        savedEntity.setSession(completedSession);
        savedEntity.setOverallScore(8);
        savedEntity.setClarityScore(8);
        savedEntity.setRelevanceScore(8);
        savedEntity.setStructureScore(8);
        savedEntity.setSummary("Solid opposition to the resolution.");
        savedEntity.setStrengths("[\"Addressed waste management issues\"]");
        savedEntity.setImprovements("[\"Mention renewable cost reductions\"]");
        savedEntity.setCreatedAt(Instant.now());

        when(feedbackRepository.save(any(Feedback.class))).thenReturn(savedEntity);

        FeedbackDto result = feedbackService.generateOrGetFeedback(10L);

        assertNotNull(result);
        verify(aiSpeakingCoachClient).analyzeSpeaking(
                eq("DEBATE"),
                eq("Nuclear energy is indispensable for net-zero targets."),
                eq("Technology"),
                eq(60),
                eq(45),
                anyString(),
                isNull(),
                eq("AGAINST")
        );
    }
}
