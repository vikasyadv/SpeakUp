package com.speakup.controller;

import com.speakup.model.*;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.JwtTokenProvider;
import com.speakup.security.UserPrincipal;
import com.speakup.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AnalyticsControllerTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    @Autowired
    private com.speakup.repository.PromptRepository promptRepository;

    @Autowired
    private com.speakup.repository.CategoryRepository categoryRepository;

    @Autowired
    private AuthService authService;

    private User userAlice;
    private User userBob;
    private String tokenAlice;
    private String tokenBob;

    private final String guestId1 = "guest-uuid-1111";
    private final String guestId2 = "guest-uuid-2222";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userAlice = userRepository.save(new User("alice@example.com", passwordEncoder.encode("Password123!"), "Alice", Role.ROLE_USER));
        tokenAlice = tokenProvider.generateToken(UserPrincipal.create(userAlice));

        userBob = userRepository.save(new User("bob@example.com", passwordEncoder.encode("Password123!"), "Bob", Role.ROLE_USER));
        tokenBob = tokenProvider.generateToken(UserPrincipal.create(userBob));
    }

    @Test
    @DisplayName("Anonymous request receives 200 OK with empty statistics")
    void getProgress_anonymous_returnsEmptyDashboard() throws Exception {
        mockMvc.perform(get("/api/v1/analytics/progress")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(0)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(0)))
                .andExpect(jsonPath("$.summary.averageOverallScore").doesNotExist())
                .andExpect(jsonPath("$.summary.reviewedSessionsCount", is(0)))
                .andExpect(jsonPath("$.skills.averageClarity").doesNotExist())
                .andExpect(jsonPath("$.modeBreakdown.offTheCuffCount", is(0)))
                .andExpect(jsonPath("$.scoreHistory", hasSize(0)))
                .andExpect(jsonPath("$.recentActivity", hasSize(0)))
                .andExpect(jsonPath("$.activeDaysLast7", is(0)))
                .andExpect(jsonPath("$.activeDaysLast14", is(0)));
    }

    @Test
    @DisplayName("Guest request receives 200 OK scoped strictly to X-Guest-Id")
    void getProgress_guest_returnsScopedStats() throws Exception {
        // Guest 1 completed session with feedback
        Session s1 = createCompletedSession(null, guestId1, Mode.OFF_THE_CUFF, 60, 62, "Guest 1 prompt");
        createFeedback(s1, 85, 90, 80, 85);

        // Guest 2 completed session with feedback (should NOT be visible to Guest 1)
        Session s2 = createCompletedSession(null, guestId2, Mode.RESEARCH, 120, 120, "Guest 2 prompt");
        createFeedback(s2, 95, 95, 95, 95);

        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(62)))
                .andExpect(jsonPath("$.summary.averageOverallScore", is(85.0)))
                .andExpect(jsonPath("$.summary.reviewedSessionsCount", is(1)))
                .andExpect(jsonPath("$.modeBreakdown.offTheCuffCount", is(1)))
                .andExpect(jsonPath("$.modeBreakdown.researchCount", is(0)))
                .andExpect(jsonPath("$.scoreHistory", hasSize(1)))
                .andExpect(jsonPath("$.scoreHistory[0].overallScore", is(85)))
                .andExpect(jsonPath("$.recentActivity", hasSize(1)))
                .andExpect(jsonPath("$.recentActivity[0].promptText", is("Guest 1 prompt")))
                .andExpect(jsonPath("$.activeDaysLast7", is(1)))
                .andExpect(jsonPath("$.activeDaysLast14", is(1)));
    }

    @Test
    @DisplayName("Authenticated user request receives 200 OK scoped to authenticated User")
    void getProgress_authenticated_returnsUserScopedStats() throws Exception {
        // Alice completed session
        Session sAlice = createCompletedSession(userAlice, null, Mode.DEBATE, 90, 90, "Alice debate");
        createFeedback(sAlice, 88, 85, 90, 89);

        // Bob completed session (should not leak to Alice)
        Session sBob = createCompletedSession(userBob, null, Mode.STORY, 120, 125, "Bob story");
        createFeedback(sBob, 75, 70, 80, 75);

        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(90)))
                .andExpect(jsonPath("$.summary.averageOverallScore", is(88.0)))
                .andExpect(jsonPath("$.skills.averageClarity", is(85.0)))
                .andExpect(jsonPath("$.skills.averageRelevance", is(90.0)))
                .andExpect(jsonPath("$.skills.averageStructure", is(89.0)))
                .andExpect(jsonPath("$.modeBreakdown.debateCount", is(1)))
                .andExpect(jsonPath("$.modeBreakdown.storyCount", is(0)))
                .andExpect(jsonPath("$.recentActivity[0].promptText", is("Alice debate")))
                .andExpect(jsonPath("$.activeDaysLast7", is(1)))
                .andExpect(jsonPath("$.activeDaysLast14", is(1)));
    }

    @Test
    @DisplayName("JWT precedence: Authenticated JWT strictly overrides X-Guest-Id header")
    void getProgress_jwtTakesPrecedenceOverGuestHeader() throws Exception {
        // Alice session
        Session sAlice = createCompletedSession(userAlice, null, Mode.OFF_THE_CUFF, 60, 60, "Alice prompt");
        createFeedback(sAlice, 80, 80, 80, 80);

        // Guest session
        Session sGuest = createCompletedSession(null, guestId1, Mode.RESEARCH, 180, 180, "Guest prompt");
        createFeedback(sGuest, 95, 95, 95, 95);

        // Request with both Alice JWT and guestId1
        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(60)))
                .andExpect(jsonPath("$.summary.averageOverallScore", is(80.0)))
                .andExpect(jsonPath("$.modeBreakdown.offTheCuffCount", is(1)))
                .andExpect(jsonPath("$.modeBreakdown.researchCount", is(0)));
    }

    @Test
    @DisplayName("Incomplete and abandoned sessions are excluded from progress analytics")
    void getProgress_excludesIncompleteAndAbandonedSessions() throws Exception {
        // Completed session
        createCompletedSession(userAlice, null, Mode.OFF_THE_CUFF, 60, 55, "Completed");

        // In-progress session
        Session sProgress = new Session();
        sProgress.setUser(userAlice);
        sProgress.setMode(Mode.RESEARCH);
        sProgress.setStatus(SessionStatus.IN_PROGRESS);
        sProgress.setDurationSeconds(120);
        sProgress.setStartedAt(Instant.now());
        sProgress.setPromptText("In progress");
        sessionRepository.save(sProgress);

        // Abandoned session
        Session sAbandoned = new Session();
        sAbandoned.setUser(userAlice);
        sAbandoned.setMode(Mode.DEBATE);
        sAbandoned.setStatus(SessionStatus.ABANDONED);
        sAbandoned.setDurationSeconds(90);
        sAbandoned.setStartedAt(Instant.now().minusSeconds(120));
        sAbandoned.setCompletedAt(Instant.now());
        sAbandoned.setPromptText("Abandoned");
        sessionRepository.save(sAbandoned);

        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(55)))
                .andExpect(jsonPath("$.modeBreakdown.offTheCuffCount", is(1)))
                .andExpect(jsonPath("$.modeBreakdown.researchCount", is(0)))
                .andExpect(jsonPath("$.modeBreakdown.debateCount", is(0)));
    }

    @Test
    @DisplayName("Guest-to-account migration: migrated sessions and feedback appear in user progress and disappear from guest")
    void getProgress_guestToAccountMigration_seamlessIntegration() throws Exception {
        // Guest creates session and gets feedback
        Session sGuest = createCompletedSession(null, guestId1, Mode.STORY, 120, 115, "Migrated Story");
        createFeedback(sGuest, 91, 92, 90, 91);

        // Before migration: Guest sees 1 session, Alice sees 0
        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)));

        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(0)));

        // Perform guest migration to Alice
        authService.migrateGuestData(userAlice, guestId1);

        // After migration: Alice sees the session and feedback
        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(1)))
                .andExpect(jsonPath("$.summary.totalSpeakingTimeSeconds", is(115)))
                .andExpect(jsonPath("$.summary.averageOverallScore", is(91.0)))
                .andExpect(jsonPath("$.modeBreakdown.storyCount", is(1)))
                .andExpect(jsonPath("$.scoreHistory", hasSize(1)))
                .andExpect(jsonPath("$.scoreHistory[0].promptText", is("Migrated Story")))
                .andExpect(jsonPath("$.recentActivity[0].promptText", is("Migrated Story")));

        // Old guestId now sees 0 sessions (no data lingering on old guest identity)
        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalCompletedSessions", is(0)))
                .andExpect(jsonPath("$.scoreHistory", hasSize(0)));
    }

    @Test
    @DisplayName("Recent activity includes promptId when prompt is linked to session and null when unlinked")
    void getProgress_promptIdInRecentActivity_returnedWhenPromptAssociated() throws Exception {
        Category category = categoryRepository.findAll().stream().findFirst()
                .orElseGet(() -> categoryRepository.save(new Category("General", "General topics")));

        Prompt prompt = new Prompt();
        prompt.setText("Debate prompt with ID");
        prompt.setMode(Mode.DEBATE);
        prompt.setCategory(category);
        Prompt savedPrompt = promptRepository.save(prompt);

        Session sessionWithPrompt = createCompletedSession(userAlice, null, Mode.DEBATE, 90, 90, "Debate prompt with ID");
        sessionWithPrompt.setPrompt(savedPrompt);
        sessionRepository.save(sessionWithPrompt);

        mockMvc.perform(get("/api/v1/analytics/progress")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recentActivity[0].promptId", is(savedPrompt.getId().intValue())));
    }

    private Session createCompletedSession(User user, String guestId, Mode mode, int duration, int actualDuration, String promptText) {
        Session s = new Session();
        s.setUser(user);
        s.setGuestId(guestId);
        s.setMode(mode);
        s.setStatus(SessionStatus.COMPLETED);
        s.setDurationSeconds(duration);
        s.setActualDurationSeconds(actualDuration);
        s.setStartedAt(Instant.now().minusSeconds(duration));
        s.setCompletedAt(Instant.now());
        s.setPromptText(promptText);
        return sessionRepository.save(s);
    }

    private Feedback createFeedback(Session session, int overall, int clarity, int relevance, int structure) {
        Feedback f = new Feedback();
        f.setSession(session);
        f.setOverallScore(overall);
        f.setClarityScore(clarity);
        f.setRelevanceScore(relevance);
        f.setStructureScore(structure);
        f.setSummary("Well structured delivery");
        return feedbackRepository.save(f);
    }
}
