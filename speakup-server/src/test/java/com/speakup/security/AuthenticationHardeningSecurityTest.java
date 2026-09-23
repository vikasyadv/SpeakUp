package com.speakup.security;

import tools.jackson.databind.ObjectMapper;
import com.speakup.dto.LoginRequestDto;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.model.*;
import com.speakup.repository.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthenticationHardeningSecurityTest {

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

    private Prompt promptX;
    private Prompt promptY;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        userA = new User("usera.hardened@example.com", passwordEncoder.encode("Password123!"), "User A", Role.ROLE_USER);
        userA = userRepository.saveAndFlush(userA);
        tokenUserA = tokenProvider.generateToken(userA.getEmail(), userA.getId(), userA.getRole().name());

        userB = new User("userb.hardened@example.com", passwordEncoder.encode("Password123!"), "User B", Role.ROLE_USER);
        userB = userRepository.saveAndFlush(userB);
        tokenUserB = tokenProvider.generateToken(userB.getEmail(), userB.getId(), userB.getRole().name());

        Category cat = categoryRepository.save(new Category("HardeningCategory", "Category for tests"));
        promptX = new Prompt();
        promptX.setText("Prompt X text");
        promptX.setCategory(cat);
        promptX.setMode(Mode.OFF_THE_CUFF);
        promptX = promptRepository.save(promptX);

        promptY = new Prompt();
        promptY.setText("Prompt Y text");
        promptY.setCategory(cat);
        promptY.setMode(Mode.OFF_THE_CUFF);
        promptY = promptRepository.save(promptY);
    }

    // =========================================================================
    // 1. JWT Security: Tampered, Expired, Malformed, Unknown User
    // =========================================================================

    @Test
    @DisplayName("Tampered JWT signature is rejected with 401 on protected endpoint and does not crash public endpoint")
    void tamperedJwt_rejectedSafely() throws Exception {
        // Create token with different key
        String foreignKey = "foreign-secret-key-that-does-not-match-speakup-test-config-at-all-256";
        String tamperedToken = Jwts.builder()
                .subject(userA.getEmail())
                .claim("userId", userA.getId())
                .claim("role", "ROLE_USER")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(foreignKey.getBytes(StandardCharsets.UTF_8)))
                .compact();

        // Protected endpoint /api/v1/auth/me -> 401 Unauthorized
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        // Public endpoint -> 200 OK without crashing
        mockMvc.perform(get("/api/v1/health")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Expired JWT returns 401 on /auth/me and falls back to guest without crashing")
    void expiredJwt_rejectedSafely() throws Exception {
        // Expired token (issued in the past, expired 1 hour ago)
        String expiredToken = Jwts.builder()
                .subject(userA.getEmail())
                .claim("userId", userA.getId())
                .claim("role", "ROLE_USER")
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(Keys.hmacShaKeyFor("speakup-test-jwt-secret-key-must-be-at-least-256-bits-long-for-hmac-sha256".getBytes(StandardCharsets.UTF_8)))
                .compact();

        // Protected endpoint -> 401
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // Public endpoint -> 200
        mockMvc.perform(get("/api/v1/health")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Malformed JWT tokens do not crash backend and are treated as unauthenticated")
    void malformedJwt_handledSafely() throws Exception {
        List<String> malformedTokens = List.of(
                "garbage.string.not.jwt",
                "eyJhbGciOiJIUzI1NiJ9.incomplete",
                "Bearer NotEvenBase64!@#$%"
        );

        for (String malformed : malformedTokens) {
            mockMvc.perform(get("/api/v1/auth/me")
                            .header("Authorization", "Bearer " + malformed))
                    .andExpect(status().isUnauthorized());

            mockMvc.perform(get("/api/v1/health")
                            .header("Authorization", "Bearer " + malformed))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @DisplayName("Valid JWT for non-existent user in database returns 401 on /auth/me")
    void validJwt_nonExistentUser_returns401() throws Exception {
        String tokenForDeletedUser = tokenProvider.generateToken("deleted.user@example.com", 99999L, "ROLE_USER");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + tokenForDeletedUser))
                .andExpect(status().isUnauthorized());
    }

    // =========================================================================
    // 2. Critical Attack Test: Authenticated User + X-Guest-Id Spoofing
    // =========================================================================

    @Test
    @DisplayName("Authenticated User A with X-Guest-Id header CANNOT access or manipulate victim guest data across all operations")
    void authenticatedUser_withXGuestIdAttack_mustNeverAccessVictimData() throws Exception {
        String victimGuestId = "victim-guest-uuid-999";

        // Victim Guest creates a session
        Session guestSession = new Session();
        guestSession.setPromptText("Victim Speaking");
        guestSession.setMode(Mode.OFF_THE_CUFF);
        guestSession.setDurationSeconds(60);
        guestSession.setActualDurationSeconds(60);
        guestSession.setStatus(SessionStatus.COMPLETED);
        guestSession.setTranscript("Victim speech transcript words for testing feedback");
        guestSession.setGuestId(victimGuestId);
        guestSession = sessionRepository.save(guestSession);

        // Victim Guest has feedback
        Feedback guestFeedback = new Feedback();
        guestFeedback.setSession(guestSession);
        guestFeedback.setOverallScore(8);
        guestFeedback.setClarityScore(8);
        guestFeedback.setRelevanceScore(8);
        guestFeedback.setStructureScore(8);
        guestFeedback.setSummary("Victim feedback summary");
        guestFeedback = feedbackRepository.save(guestFeedback);

        // Victim Guest bookmarks Prompt X
        Bookmark guestBookmark = new Bookmark();
        guestBookmark.setPrompt(promptX);
        guestBookmark.setGuestId(victimGuestId);
        guestBookmark = bookmarkRepository.save(guestBookmark);

        // --- ATTACK ATTEMPTS BY USER A SUPPLYING victimGuestId in X-Guest-Id ---

        // 1. GET session: User A tries to view victim's session -> 404
        mockMvc.perform(get("/api/v1/sessions/" + guestSession.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));

        // 2. Complete session: User A tries to complete victim's session -> 404
        SessionCompleteDto completeDto = new SessionCompleteDto("Hacked transcript", 60);
        mockMvc.perform(patch("/api/v1/sessions/" + guestSession.getId() + "/complete")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeDto)))
                .andExpect(status().isNotFound());

        // 3. Abandon session: User A tries to abandon victim's session -> 404
        mockMvc.perform(patch("/api/v1/sessions/" + guestSession.getId() + "/abandon")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound());

        // 4. Delete session: User A tries to delete victim's session -> 404
        mockMvc.perform(delete("/api/v1/sessions/" + guestSession.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound());
        // Verify victim session was NOT deleted
        assertThat(sessionRepository.existsById(guestSession.getId())).isTrue();

        // 5. GET feedback: User A tries to fetch victim's feedback -> 404
        mockMvc.perform(get("/api/v1/sessions/" + guestSession.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound());

        // 6. POST feedback: User A tries to generate feedback for victim's session -> 404
        mockMvc.perform(post("/api/v1/sessions/" + guestSession.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound());

        // 7. Check bookmark: User A checks if Prompt X is bookmarked -> false (victim has it, but User A does not)
        mockMvc.perform(get("/api/v1/bookmarks/check/" + promptX.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookmarked").value(false));

        // 8. Delete bookmark: User A tries to delete victim's bookmark -> 404
        mockMvc.perform(delete("/api/v1/bookmarks/" + promptX.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isNotFound());
        // Verify victim bookmark was NOT deleted
        assertThat(bookmarkRepository.existsById(guestBookmark.getId())).isTrue();

        // 9. List bookmarks: User A gets bookmark list -> does NOT contain victim's bookmark
        mockMvc.perform(get("/api/v1/bookmarks")
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // 10. Create bookmark: User A bookmarks Prompt Y while sending victimGuestId -> owned by User A, NOT victim
        mockMvc.perform(post("/api/v1/bookmarks/" + promptY.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", victimGuestId))
                .andExpect(status().isCreated());

        Bookmark createdBookmark = bookmarkRepository.findByPromptIdAndUser(promptY.getId(), userA).orElseThrow();
        assertThat(createdBookmark.getUser().getId()).isEqualTo(userA.getId());
        assertThat(createdBookmark.getGuestId()).isNull();
    }

    // =========================================================================
    // 3. IDOR Test Matrix
    // =========================================================================

    @Test
    @DisplayName("IDOR Matrix: Verifies access permissions across all Caller vs Owner combinations")
    void idorTestMatrix_sessionVerification() throws Exception {
        String guestA = "guest-matrix-A";
        String guestB = "guest-matrix-B";

        // Resource 1: Owned by Guest A
        Session sessionGuestA = new Session();
        sessionGuestA.setPromptText("Guest A Topic");
        sessionGuestA.setMode(Mode.OFF_THE_CUFF);
        sessionGuestA.setDurationSeconds(60);
        sessionGuestA.setGuestId(guestA);
        sessionGuestA = sessionRepository.save(sessionGuestA);

        // Resource 2: Owned by User A
        Session sessionUserA = new Session();
        sessionUserA.setPromptText("User A Topic");
        sessionUserA.setMode(Mode.OFF_THE_CUFF);
        sessionUserA.setDurationSeconds(60);
        sessionUserA.setUser(userA);
        sessionUserA = sessionRepository.save(sessionUserA);

        // 1. Guest A accesses Guest A -> ALLOW (200)
        mockMvc.perform(get("/api/v1/sessions/" + sessionGuestA.getId())
                        .header("X-Guest-Id", guestA))
                .andExpect(status().isOk());

        // 2. Guest B accesses Guest A -> 404
        mockMvc.perform(get("/api/v1/sessions/" + sessionGuestA.getId())
                        .header("X-Guest-Id", guestB))
                .andExpect(status().isNotFound());

        // 3. Guest A accesses User A -> 404
        mockMvc.perform(get("/api/v1/sessions/" + sessionUserA.getId())
                        .header("X-Guest-Id", guestA))
                .andExpect(status().isNotFound());

        // 4. User A accesses User A -> ALLOW (200)
        mockMvc.perform(get("/api/v1/sessions/" + sessionUserA.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isOk());

        // 5. User B accesses User A -> 404
        mockMvc.perform(get("/api/v1/sessions/" + sessionUserA.getId())
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());

        // 6. User A accesses Guest A -> 404
        mockMvc.perform(get("/api/v1/sessions/" + sessionGuestA.getId())
                        .header("Authorization", "Bearer " + tokenUserA))
                .andExpect(status().isNotFound());

        // 7. User A + Guest B header accesses User A -> ALLOW (200)
        mockMvc.perform(get("/api/v1/sessions/" + sessionUserA.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", guestB))
                .andExpect(status().isOk());

        // 8. User A + Guest B header accesses Guest A -> 404
        mockMvc.perform(get("/api/v1/sessions/" + sessionGuestA.getId())
                        .header("Authorization", "Bearer " + tokenUserA)
                        .header("X-Guest-Id", guestB))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 4. Comprehensive Guest-to-Account Migration Verification
    // =========================================================================

    @Test
    @DisplayName("Guest-to-Account Registration Migration: migrates multiple sessions, AI feedback, deduplicates bookmarks")
    void fullGuestRegistrationMigration_withMultipleSessionsAndFeedbackAndDuplicateBookmarks() throws Exception {
        String guestId = "guest-full-migration-uuid";

        // Guest has 2 completed sessions
        Session session1 = new Session();
        session1.setPromptText("Guest Topic 1");
        session1.setMode(Mode.OFF_THE_CUFF);
        session1.setDurationSeconds(60);
        session1.setActualDurationSeconds(60);
        session1.setStatus(SessionStatus.COMPLETED);
        session1.setTranscript("Completed transcript for session 1");
        session1.setGuestId(guestId);
        session1 = sessionRepository.save(session1);

        Session session2 = new Session();
        session2.setPromptText("Guest Topic 2");
        session2.setMode(Mode.RESEARCH);
        session2.setDurationSeconds(120);
        session2.setActualDurationSeconds(120);
        session2.setStatus(SessionStatus.COMPLETED);
        session2.setTranscript("Completed transcript for session 2");
        session2.setGuestId(guestId);
        session2 = sessionRepository.save(session2);

        // Session 1 has AI Feedback
        Feedback feedback1 = new Feedback();
        feedback1.setSession(session1);
        feedback1.setOverallScore(9);
        feedback1.setClarityScore(9);
        feedback1.setRelevanceScore(9);
        feedback1.setStructureScore(9);
        feedback1.setSummary("Great story structure");
        feedback1 = feedbackRepository.save(feedback1);

        // Guest has 2 bookmarks: Prompt X and Prompt Y
        Bookmark bmX = new Bookmark();
        bmX.setPrompt(promptX);
        bmX.setGuestId(guestId);
        bmX = bookmarkRepository.save(bmX);

        Bookmark bmY = new Bookmark();
        bmY.setPrompt(promptY);
        bmY.setGuestId(guestId);
        bmY = bookmarkRepository.save(bmY);

        // Verify BEFORE state: user == null, guestId == guestId
        assertThat(session1.getUser()).isNull();
        assertThat(session1.getGuestId()).isEqualTo(guestId);
        assertThat(bmX.getUser()).isNull();
        assertThat(bmX.getGuestId()).isEqualTo(guestId);

        // Guest registers account
        RegisterRequestDto registerDto = new RegisterRequestDto("new.migrated.user@example.com", "Password123!", "Migrated User");
        String registerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Guest-Id", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(registerJson).get("token").asText();
        Long newUserId = objectMapper.readTree(registerJson).get("user").get("id").asLong();

        // Verify AFTER state:
        // 1. Sessions: user = newUserId, guestId = null
        Session afterSession1 = sessionRepository.findById(session1.getId()).orElseThrow();
        assertThat(afterSession1.getUser().getId()).isEqualTo(newUserId);
        assertThat(afterSession1.getGuestId()).isNull();

        Session afterSession2 = sessionRepository.findById(session2.getId()).orElseThrow();
        assertThat(afterSession2.getUser().getId()).isEqualTo(newUserId);
        assertThat(afterSession2.getGuestId()).isNull();

        // 2. Feedback: still attached to session1, retrievable by new user
        mockMvc.perform(get("/api/v1/sessions/" + session1.getId() + "/feedback")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallScore").value(9))
                .andExpect(jsonPath("$.summary").value("Great story structure"));

        // User B cannot access this feedback -> 404
        mockMvc.perform(get("/api/v1/sessions/" + session1.getId() + "/feedback")
                        .header("Authorization", "Bearer " + tokenUserB))
                .andExpect(status().isNotFound());

        // 3. Bookmarks: owned by new user
        Bookmark afterBmX = bookmarkRepository.findById(bmX.getId()).orElseThrow();
        assertThat(afterBmX.getUser().getId()).isEqualTo(newUserId);
        assertThat(afterBmX.getGuestId()).isNull();

        Bookmark afterBmY = bookmarkRepository.findById(bmY.getId()).orElseThrow();
        assertThat(afterBmY.getUser().getId()).isEqualTo(newUserId);
        assertThat(afterBmY.getGuestId()).isNull();

        // 4. Old guest ID can no longer access sessions or bookmarks -> 404 / empty list
        mockMvc.perform(get("/api/v1/sessions/" + session1.getId())
                        .header("X-Guest-Id", guestId))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/bookmarks")
                        .header("X-Guest-Id", guestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Login migration: merges data into existing account and deduplicates conflicting bookmarks")
    void loginMigration_mergesDataAndDeduplicatesConflict() throws Exception {
        String guestId = "guest-login-migrator";

        // User A ALREADY owns a bookmark for Prompt X
        Bookmark existingUserABookmark = new Bookmark();
        existingUserABookmark.setPrompt(promptX);
        existingUserABookmark.setUser(userA);
        existingUserABookmark = bookmarkRepository.save(existingUserABookmark);

        // Guest A bookmarks Prompt X (conflict!) and Prompt Y (new)
        Bookmark guestBmX = new Bookmark();
        guestBmX.setPrompt(promptX);
        guestBmX.setGuestId(guestId);
        guestBmX = bookmarkRepository.save(guestBmX);

        Bookmark guestBmY = new Bookmark();
        guestBmY.setPrompt(promptY);
        guestBmY.setGuestId(guestId);
        guestBmY = bookmarkRepository.save(guestBmY);

        // Guest A has Session
        Session guestSession = new Session();
        guestSession.setPromptText("Guest Session before login");
        guestSession.setMode(Mode.DEBATE);
        guestSession.setDurationSeconds(90);
        guestSession.setGuestId(guestId);
        guestSession = sessionRepository.save(guestSession);

        // User A logs in while passing X-Guest-Id
        LoginRequestDto loginDto = new LoginRequestDto(userA.getEmail(), "Password123!");
        String loginJson = mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Guest-Id", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(loginJson).get("token").asText();

        // Verify:
        // 1. Session is now owned by User A
        Session migratedSession = sessionRepository.findById(guestSession.getId()).orElseThrow();
        assertThat(migratedSession.getUser().getId()).isEqualTo(userA.getId());
        assertThat(migratedSession.getGuestId()).isNull();

        // 2. Duplicate guest bookmark for Prompt X was removed (only 1 bookmark exists for Prompt X and User A)
        List<Bookmark> userABookmarksForX = bookmarkRepository.findByUserOrderByCreatedAtDesc(userA).stream()
                .filter(b -> b.getPrompt().getId().equals(promptX.getId()))
                .toList();
        assertThat(userABookmarksForX).hasSize(1);
        assertThat(userABookmarksForX.get(0).getId()).isEqualTo(existingUserABookmark.getId());

        // 3. Prompt Y was migrated to User A
        Bookmark migratedBmY = bookmarkRepository.findById(guestBmY.getId()).orElseThrow();
        assertThat(migratedBmY.getUser().getId()).isEqualTo(userA.getId());
        assertThat(migratedBmY.getGuestId()).isNull();

        // 4. Authenticated request sees both Prompt X and Prompt Y
        mockMvc.perform(get("/api/v1/bookmarks")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Migration is guest-specific: Guest A registration does NOT affect Guest B data")
    void migration_guestSpecific_doesNotAffectOtherGuests() throws Exception {
        String guestA = "guest-migrator-A";
        String guestB = "guest-migrator-B";

        // Guest A data
        Session sessionA = new Session();
        sessionA.setPromptText("Guest A Topic");
        sessionA.setMode(Mode.OFF_THE_CUFF);
        sessionA.setDurationSeconds(60);
        sessionA.setGuestId(guestA);
        sessionA = sessionRepository.save(sessionA);

        Bookmark bmA = new Bookmark();
        bmA.setPrompt(promptX);
        bmA.setGuestId(guestA);
        bmA = bookmarkRepository.save(bmA);

        // Guest B data
        Session sessionB = new Session();
        sessionB.setPromptText("Guest B Topic");
        sessionB.setMode(Mode.OFF_THE_CUFF);
        sessionB.setDurationSeconds(60);
        sessionB.setGuestId(guestB);
        sessionB = sessionRepository.save(sessionB);

        Bookmark bmB = new Bookmark();
        bmB.setPrompt(promptY);
        bmB.setGuestId(guestB);
        bmB = bookmarkRepository.save(bmB);

        // Guest A registers
        RegisterRequestDto regDto = new RegisterRequestDto("guestA.isolated@example.com", "Password123!", "Guest A User");
        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Guest-Id", guestA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regDto)))
                .andExpect(status().isCreated());

        // Guest A session is migrated
        Session updatedSessionA = sessionRepository.findById(sessionA.getId()).orElseThrow();
        assertThat(updatedSessionA.getUser()).isNotNull();
        assertThat(updatedSessionA.getGuestId()).isNull();

        // Guest B session is UNTOUCHED
        Session updatedSessionB = sessionRepository.findById(sessionB.getId()).orElseThrow();
        assertThat(updatedSessionB.getUser()).isNull();
        assertThat(updatedSessionB.getGuestId()).isEqualTo(guestB);

        // Guest B bookmark is UNTOUCHED
        Bookmark updatedBmB = bookmarkRepository.findById(bmB.getId()).orElseThrow();
        assertThat(updatedBmB.getUser()).isNull();
        assertThat(updatedBmB.getGuestId()).isEqualTo(guestB);

        // Guest B can still access their session
        mockMvc.perform(get("/api/v1/sessions/" + sessionB.getId())
                        .header("X-Guest-Id", guestB))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login without X-Guest-Id succeeds normally without performing migration")
    void login_withoutGuestId_succeedsWithoutMigration() throws Exception {
        LoginRequestDto loginDto = new LoginRequestDto(userA.getEmail(), "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.email").value(userA.getEmail()));
    }
}
