package com.speakup.security;

import tools.jackson.databind.ObjectMapper;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.model.*;
import com.speakup.repository.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OwnershipIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FeedbackRepository feedbackRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    private User userA;
    private User userB;
    private String tokenUserA;
    private String tokenUserB;
    private Prompt prompt1;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userA = new User("usera.owner@example.com", passwordEncoder.encode("Pass1234!"), "User A", Role.ROLE_USER);
        userA = userRepository.saveAndFlush(userA);
        tokenUserA = tokenProvider.generateToken(userA.getEmail(), userA.getId(), userA.getRole().name());

        userB = new User("userb.owner@example.com", passwordEncoder.encode("Pass1234!"), "User B", Role.ROLE_USER);
        userB = userRepository.saveAndFlush(userB);
        tokenUserB = tokenProvider.generateToken(userB.getEmail(), userB.getId(), userB.getRole().name());

        Category cat = categoryRepository.save(new Category("Debate", "Debate topics"));
        prompt1 = new Prompt();
        prompt1.setText("Universal Basic Income");
        prompt1.setCategory(cat);
        prompt1.setMode(Mode.DEBATE);
        prompt1 = promptRepository.save(prompt1);
    }

    @Test
    @DisplayName("User A can create and access own session, but User B gets 404 when accessing it")
    void sessionOwnership_userAAndUserB_isolation() throws Exception {
        SessionCreateDto createDto = new SessionCreateDto("Universal Basic Income", prompt1.getId(), "DEBATE", 120);

        // User A creates session
        String responseContent = mockMvc.perform(post("/api/v1/sessions")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();

        Long sessionId = objectMapper.readTree(responseContent).get("id").asLong();

        // User A accesses own session -> 200 OK
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptText").value("Universal Basic Income"));

        // User B attempts to access User A's session -> 404 Not Found (no IDOR)
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // Guest attempts to access User A's session -> 404 Not Found
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("X-Guest-Id", "guest-intruder-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Guest A can create and access own session, but Guest B gets 404")
    void sessionOwnership_guestAGuestB_isolation() throws Exception {
        String guestIdA = "guest-owner-A";
        String guestIdB = "guest-owner-B";

        SessionCreateDto createDto = new SessionCreateDto("Guest Speaking Topic", prompt1.getId(), "OFF_THE_CUFF", 60);

        // Guest A creates session
        String responseContent = mockMvc.perform(post("/api/v1/sessions")
                        .header("X-Guest-Id", guestIdA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long sessionId = objectMapper.readTree(responseContent).get("id").asLong();

        // Guest A accesses own session -> 200 OK
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("X-Guest-Id", guestIdA))
                .andExpect(status().isOk());

        // Guest B accesses Guest A's session -> 404 Not Found
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("X-Guest-Id", guestIdB))
                .andExpect(status().isNotFound());

        // Authenticated user accesses Guest A's session -> 404 Not Found
        mockMvc.perform(get("/api/v1/sessions/" + sessionId)
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Authenticated identity takes precedence over X-Guest-Id header")
    void authenticatedPrecedence_ignoresXGuestId() throws Exception {
        Session session = new Session();
        session.setPromptText("User A Session");
        session.setMode(Mode.OFF_THE_CUFF);
        session.setDurationSeconds(60);
        session.setUser(userA);
        session.setGuestId(null);
        session = sessionRepository.save(session);

        // Request sends User A JWT + a different X-Guest-Id header
        // Since User A is authenticated, User A is authoritative -> 200 OK
        mockMvc.perform(get("/api/v1/sessions/" + session.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", "some-other-guest-id"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptText").value("User A Session"));
    }

    @Test
    @DisplayName("User A and User B can independently bookmark the same prompt")
    void bookmarkOwnership_independentMultiUserBookmarks() throws Exception {
        // User A bookmarks prompt1
        mockMvc.perform(post("/api/v1/bookmarks/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isCreated());

        // User B independently bookmarks prompt1
        mockMvc.perform(post("/api/v1/bookmarks/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isCreated());

        // User A sees prompt1 as bookmarked
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // User B sees prompt1 as bookmarked
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // User A removes their bookmark
        mockMvc.perform(delete("/api/v1/bookmarks/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNoContent());

        // User A no longer has it bookmarked
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(false));

        // User B STILL has it bookmarked (User A's deletion did not affect User B!)
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));
    }

    @Test
    @DisplayName("Guest A and Guest B can independently bookmark the same prompt")
    void bookmarkOwnership_independentMultiGuestBookmarks() throws Exception {
        String guestIdA = "guest-bookmark-A";
        String guestIdB = "guest-bookmark-B";

        // Guest A bookmarks
        mockMvc.perform(post("/api/v1/bookmarks/" + prompt1.getId())
                        .header("X-Guest-Id", guestIdA))
                .andExpect(status().isCreated());

        // Guest B bookmarks
        mockMvc.perform(post("/api/v1/bookmarks/" + prompt1.getId())
                        .header("X-Guest-Id", guestIdB))
                .andExpect(status().isCreated());

        // Guest A check -> true
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("X-Guest-Id", guestIdA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // Guest B check -> true
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("X-Guest-Id", guestIdB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(true));

        // Guest C (who hasn't bookmarked) check -> false
        mockMvc.perform(get("/api/v1/bookmarks/check/" + prompt1.getId())
                        .header("X-Guest-Id", "guest-bookmark-C"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(false));
    }

    @Test
    @DisplayName("Feedback access requires ownership of parent session; other callers receive 404")
    void feedbackOwnership_isolation() throws Exception {
        // Completed session owned by User A
        Session session = new Session();
        session.setPromptText("Feedback test");
        session.setMode(Mode.OFF_THE_CUFF);
        session.setDurationSeconds(60);
        session.setActualDurationSeconds(55);
        session.setStatus(SessionStatus.COMPLETED);
        session.setTranscript("This is a complete transcript of speaking practice");
        session.setUser(userA);
        session.setGuestId(null);
        session = sessionRepository.save(session);

        // Pre-create feedback
        Feedback feedback = new Feedback();
        feedback.setSession(session);
        feedback.setOverallScore(9);
        feedback.setClarityScore(9);
        feedback.setRelevanceScore(9);
        feedback.setStructureScore(9);
        feedback.setSummary("Superb speech");
        feedback = feedbackRepository.save(feedback);

        // User A gets feedback -> 200 OK
        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(9));

        // User B attempts to get User A's feedback -> 404 Not Found
        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());

        // Guest attempts to get User A's feedback -> 404 Not Found
        mockMvc.perform(get("/api/v1/sessions/" + session.getId() + "/feedback")
                        .header("X-Guest-Id", "guest-uuid-intruder"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Guest data migration attaches records to new account and clears guestId")
    void migration_endToEndOwnershipTransfer() throws Exception {
        String guestId = "guest-migration-e2e";

        // Create guest session
        Session guestSession = new Session();
        guestSession.setPromptText("Guest Practice Session");
        guestSession.setMode(Mode.OFF_THE_CUFF);
        guestSession.setDurationSeconds(60);
        guestSession.setGuestId(guestId);
        guestSession = sessionRepository.save(guestSession);

        // Create guest bookmark
        Bookmark guestBookmark = new Bookmark();
        guestBookmark.setPrompt(prompt1);
        guestBookmark.setGuestId(guestId);
        guestBookmark = bookmarkRepository.save(guestBookmark);

        // Register user passing X-Guest-Id
        RegisterRequestDto regDto = new RegisterRequestDto("migrated.user@example.com", "Password123!", "Migrated User");
        String regResponse = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Guest-Id", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(regResponse).get("token").asText();

        // Verify session now accessible via the new user's JWT
        mockMvc.perform(get("/api/v1/sessions/" + guestSession.getId())
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptText").value("Guest Practice Session"));

        // Verify database state: user != null, guestId == null (exclusive ownership)
        Session updatedSession = sessionRepository.findById(guestSession.getId()).orElseThrow();
        assertThat(updatedSession.getUser()).isNotNull();
        assertThat(updatedSession.getGuestId()).isNull();

        Bookmark updatedBookmark = bookmarkRepository.findById(guestBookmark.getId()).orElseThrow();
        assertThat(updatedBookmark.getUser()).isNotNull();
        assertThat(updatedBookmark.getGuestId()).isNull();

        // Old guest ID cannot access the migrated session anymore -> 404
        mockMvc.perform(get("/api/v1/sessions/" + guestSession.getId())
                        .header("X-Guest-Id", guestId))
                .andExpect(status().isNotFound());
    }
}
