package com.speakup.controller;

import com.speakup.model.*;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.FeedbackRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.JwtTokenProvider;
import com.speakup.security.UserPrincipal;
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
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SessionControllerTest {

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
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private User userAlice;
    private User userBob;
    private String tokenAlice;
    private String tokenBob;

    private Category categoryTech;
    private Category categoryPhilosophy;
    private Prompt promptAi;
    private Prompt promptEthics;

    private final String guestId1 = "guest-uuid-1111";
    private final String guestId2 = "guest-uuid-2222";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userAlice = userRepository.findByEmail("alice.sessions@example.com")
                .orElseGet(() -> userRepository.save(new User("alice.sessions@example.com", passwordEncoder.encode("Password123!"), "Alice", Role.ROLE_USER)));
        tokenAlice = tokenProvider.generateToken(UserPrincipal.create(userAlice));

        userBob = userRepository.findByEmail("bob.sessions@example.com")
                .orElseGet(() -> userRepository.save(new User("bob.sessions@example.com", passwordEncoder.encode("Password123!"), "Bob", Role.ROLE_USER)));
        tokenBob = tokenProvider.generateToken(UserPrincipal.create(userBob));

        categoryTech = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(new Category("Technology", "Tech topics")));
        categoryPhilosophy = categoryRepository.findByName("Philosophy")
                .orElseGet(() -> categoryRepository.save(new Category("Philosophy", "Philosophy topics")));

        promptAi = new Prompt();
        promptAi.setText("Explain Neural Networks");
        promptAi.setCategory(categoryTech);
        promptAi.setMode(Mode.OFF_THE_CUFF);
        promptAi.setActive(true);
        promptAi = promptRepository.save(promptAi);

        promptEthics = new Prompt();
        promptEthics.setText("Is Utilitarianism Justified?");
        promptEthics.setCategory(categoryPhilosophy);
        promptEthics.setMode(Mode.DEBATE);
        promptEthics.setActive(true);
        promptEthics = promptRepository.save(promptEthics);
    }

    private Session createSession(User user, String guestId, Mode mode, SessionStatus status,
                                  String promptText, Prompt prompt, String transcript,
                                  String notes, Instant startedAt) {
        Session s = new Session();
        s.setUser(user);
        s.setGuestId(guestId);
        s.setMode(mode);
        s.setStatus(status);
        s.setPromptText(promptText);
        s.setPrompt(prompt);
        s.setTranscript(transcript);
        s.setPreparationNotes(notes);
        s.setDurationSeconds(120);
        s.setActualDurationSeconds(115);
        s.setStartedAt(startedAt);
        if (status == SessionStatus.COMPLETED) {
            s.setCompletedAt(startedAt.plus(115, ChronoUnit.SECONDS));
        }
        return sessionRepository.save(s);
    }

    // 1. Authenticated user pagination
    @Test
    @DisplayName("1. Authenticated user receives paginated sessions with total elements and pages")
    void getSessions_authenticatedUserPagination_succeeds() throws Exception {
        Instant baseTime = Instant.now().minus(10, ChronoUnit.DAYS);
        for (int i = 0; i < 5; i++) {
            createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED,
                    "Topic " + i, promptAi, "Transcript " + i, null, baseTime.plus(i, ChronoUnit.HOURS));
        }

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(5)))
                .andExpect(jsonPath("$.totalPages", is(3)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(2)));

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("page", "2")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.number", is(2)));
    }

    // 2. Guest pagination
    @Test
    @DisplayName("2. Guest caller receives paginated guest sessions")
    void getSessions_guestPagination_succeeds() throws Exception {
        Instant baseTime = Instant.now().minus(5, ChronoUnit.DAYS);
        for (int i = 0; i < 3; i++) {
            createSession(null, guestId1, Mode.RESEARCH, SessionStatus.COMPLETED,
                    "Guest Topic " + i, null, "Transcript " + i, "Notes " + i, baseTime.plus(i, ChronoUnit.HOURS));
        }

        mockMvc.perform(get("/api/v1/sessions")
                        .header("X-Guest-Id", guestId1)
                        .param("page", "0")
                        .param("size", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(3)))
                .andExpect(jsonPath("$.totalPages", is(2)))
                .andExpect(jsonPath("$.number", is(0)));
    }

    // 3. Mode filtering
    @Test
    @DisplayName("3. Mode filtering filters sessions by exact mode")
    void getSessions_modeFiltering_returnsOnlyMatchingMode() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Off Cuff 1", null, "Speech 1", null, now.minus(3, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.RESEARCH, SessionStatus.COMPLETED, "Research 1", null, "Speech 2", "Notes", now.minus(2, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.DEBATE, SessionStatus.COMPLETED, "Debate 1", null, "Speech 3", null, now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("mode", "RESEARCH")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].mode", is("RESEARCH")))
                .andExpect(jsonPath("$.content[0].promptText", is("Research 1")));
    }

    // 4. Status filtering
    @Test
    @DisplayName("4. Status filtering filters sessions by session status")
    void getSessions_statusFiltering_returnsOnlyMatchingStatus() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.IN_PROGRESS, "In Progress Speech", null, null, null, now.minus(3, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Completed Speech", null, "Done", null, now.minus(2, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.ABANDONED, "Abandoned Speech", null, null, null, now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("status", "COMPLETED")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is("COMPLETED")))
                .andExpect(jsonPath("$.content[0].promptText", is("Completed Speech")));
    }

    // 5. Keyword search (promptText, transcript, preparationNotes, category)
    @Test
    @DisplayName("5. Keyword search matches across promptText, transcript, notes, and category")
    void getSessions_keywordSearch_matchesVariousFields() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "The Future of Quantum Computing", null, "Generic words here", null, now.minus(4, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Standard Topic", null, "Talking about astrophysics and gravitational waves", null, now.minus(3, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.RESEARCH, SessionStatus.COMPLETED, "General Talk", null, "Speech text", "Deep-dive outline-scratchpad key point", now.minus(2, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.DEBATE, SessionStatus.COMPLETED, "Moral Questions", promptEthics, "Debate arguments", null, now.minus(1, ChronoUnit.HOURS));

        // Search matching promptText
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("search", "quantum")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", containsString("Quantum")));

        // Search matching transcript
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("search", "gravitational")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].transcript", containsString("gravitational")));

        // Search matching notes
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("search", "outline-scratchpad")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].preparationNotes", containsString("outline-scratchpad")));

        // Search matching prompt category name ("Philosophy")
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("search", "philosophy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Moral Questions")));
    }

    // 6. Combined filters (mode + status + search)
    @Test
    @DisplayName("6. Combined filters mode + status + search work together")
    void getSessions_combinedFilters_returnsSpecificSession() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.DEBATE, SessionStatus.COMPLETED, "Climate Policy Debate", null, "Carbon taxes are essential", null, now.minus(3, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.DEBATE, SessionStatus.IN_PROGRESS, "Climate Policy Debate", null, null, null, now.minus(2, ChronoUnit.HOURS));
        createSession(userAlice, null, Mode.RESEARCH, SessionStatus.COMPLETED, "Climate Policy Research", null, "Researching climate", null, now.minus(1, ChronoUnit.HOURS));

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("mode", "DEBATE")
                        .param("status", "COMPLETED")
                        .param("search", "carbon")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].mode", is("DEBATE")))
                .andExpect(jsonPath("$.content[0].status", is("COMPLETED")))
                .andExpect(jsonPath("$.content[0].transcript", containsString("Carbon taxes")));
    }

    // 7. Empty search results
    @Test
    @DisplayName("7. Non-matching search query returns empty page with totalElements 0")
    void getSessions_emptySearchResults_returnsEmptyPage() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Sample Speech", null, "Speech text", null, now);

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("search", "completely_unmatched_term_xyz_12345")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)))
                .andExpect(jsonPath("$.empty", is(true)));
    }

    // 8. Cross-user/guest isolation
    @Test
    @DisplayName("8. Strict cross-user and cross-guest isolation is enforced")
    void getSessions_crossUserGuestIsolation_verified() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Alice Exclusive Topic", null, "Alice speech", null, now);
        createSession(userBob, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Bob Exclusive Topic", null, "Bob speech", null, now);
        createSession(null, guestId1, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Guest 1 Exclusive Topic", null, "Guest 1 speech", null, now);
        createSession(null, guestId2, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Guest 2 Exclusive Topic", null, "Guest 2 speech", null, now);

        // Alice sees ONLY Alice's sessions
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Alice Exclusive Topic")));

        // Bob sees ONLY Bob's sessions
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenBob)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Bob Exclusive Topic")));

        // Guest 1 sees ONLY Guest 1's sessions
        mockMvc.perform(get("/api/v1/sessions")
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Guest 1 Exclusive Topic")));

        // Guest 2 sees ONLY Guest 2's sessions
        mockMvc.perform(get("/api/v1/sessions")
                        .header("X-Guest-Id", guestId2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Guest 2 Exclusive Topic")));

        // Auth token takes precedence over X-Guest-Id header
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .header("X-Guest-Id", guestId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].promptText", is("Alice Exclusive Topic")));
    }

    // 9. Backward-compatible request with no parameters
    @Test
    @DisplayName("9. Request with no query parameters returns 200 OK with default pagination")
    void getSessions_noParameters_returnsDefaultPageable() throws Exception {
        Instant now = Instant.now();
        createSession(userAlice, null, Mode.OFF_THE_CUFF, SessionStatus.COMPLETED, "Default Test Topic", null, "Speech text", null, now);

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.size", is(20)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.totalPages", is(1)));
    }

    // 10. Invalid filter values return 400 Bad Request
    @Test
    @DisplayName("10. Invalid mode or status returns 400 Bad Request")
    void getSessions_invalidFilters_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("mode", "NOT_A_VALID_MODE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid mode: 'NOT_A_VALID_MODE'")));

        mockMvc.perform(get("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenAlice)
                        .param("status", "NOT_A_VALID_STATUS")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Invalid status: 'NOT_A_VALID_STATUS'")));
    }
}
