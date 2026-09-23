package com.speakup.controller;

import tools.jackson.databind.ObjectMapper;
import com.speakup.dto.LoginRequestDto;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.model.*;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.JwtTokenProvider;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired
    private WebApplicationContext context;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("POST /register succeeds with valid payload, hashes password, and returns 201 with JWT")
    void register_success() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("newuser@example.com", "Password123!", "New User");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("newuser@example.com"))
                .andExpect(jsonPath("$.user.displayName").value("New User"))
                .andExpect(jsonPath("$.user.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.user.id").isNumber());

        User user = userRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(user.getDisplayName()).isEqualTo("New User");
        assertThat(passwordEncoder.matches("Password123!", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("POST /register fails with 400 when email format is invalid")
    void register_invalidEmail_returns400() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("not-an-email", "Password123!", "Invalid Email");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /register fails with 400 when password is under 8 characters")
    void register_shortPassword_returns400() throws Exception {
        RegisterRequestDto request = new RegisterRequestDto("shortpass@example.com", "12345", "Short Pass");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /register fails with 400 when email is already registered")
    void register_duplicateEmail_returns400() throws Exception {
        User existing = new User("duplicate@example.com", passwordEncoder.encode("Password123!"), "Existing", Role.ROLE_USER);
        userRepository.saveAndFlush(existing);

        RegisterRequestDto request = new RegisterRequestDto("duplicate@example.com", "Password123!", "Duplicate");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    @DisplayName("POST /login succeeds with correct credentials, returning 200 with JWT")
    void login_success() throws Exception {
        User user = new User("loginuser@example.com", passwordEncoder.encode("CorrectPassword!"), "Login User", Role.ROLE_USER);
        userRepository.saveAndFlush(user);

        LoginRequestDto request = new LoginRequestDto("loginuser@example.com", "CorrectPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value("loginuser@example.com"));
    }

    @Test
    @DisplayName("POST /login returns 401 when password is incorrect")
    void login_wrongPassword_returns401() throws Exception {
        User user = new User("wrongpass@example.com", passwordEncoder.encode("CorrectPassword!"), "Wrong Pass", Role.ROLE_USER);
        userRepository.saveAndFlush(user);

        LoginRequestDto request = new LoginRequestDto("wrongpass@example.com", "IncorrectPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /login returns 401 when email does not exist")
    void login_nonExistentUser_returns401() throws Exception {
        LoginRequestDto request = new LoginRequestDto("nobody@example.com", "SomePassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /me returns 401 Unauthorized when unauthenticated")
    void me_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /me returns 401 Unauthorized when token is invalid")
    void me_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer invalid-token-xyz"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /me returns 200 with UserDto when valid Bearer token is provided")
    void me_validToken_returnsUserDto() throws Exception {
        User user = new User("metest@example.com", passwordEncoder.encode("Password123!"), "Me Tester", Role.ROLE_USER);
        user = userRepository.saveAndFlush(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("metest@example.com"))
                .andExpect(jsonPath("$.displayName").value("Me Tester"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.id").value(user.getId()));
    }

    @Test
    @DisplayName("POST /register with X-Guest-Id migrates guest sessions and bookmarks to the new account")
    void register_withXGuestId_migratesData() throws Exception {
        String guestId = "guest-register-test-uuid";

        Category category = categoryRepository.save(new Category("Cat1", "Desc"));
        Prompt prompt1 = new Prompt();
        prompt1.setText("Prompt 1");
        prompt1.setCategory(category);
        prompt1.setMode(Mode.OFF_THE_CUFF);
        prompt1 = promptRepository.save(prompt1);

        // Guest session
        Session guestSession = new Session();
        guestSession.setPrompt(prompt1);
        guestSession.setPromptText(prompt1.getText());
        guestSession.setMode(Mode.OFF_THE_CUFF);
        guestSession.setDurationSeconds(60);
        guestSession.setActualDurationSeconds(45);
        guestSession.setGuestId(guestId);
        guestSession = sessionRepository.save(guestSession);

        // Guest bookmark
        Bookmark guestBookmark = new Bookmark();
        guestBookmark.setPrompt(prompt1);
        guestBookmark.setGuestId(guestId);
        guestBookmark = bookmarkRepository.save(guestBookmark);

        RegisterRequestDto request = new RegisterRequestDto("guestclaim@example.com", "Password123!", "Claimer");

        mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Guest-Id", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email").value("guestclaim@example.com"));

        User newUser = userRepository.findByEmail("guestclaim@example.com").orElseThrow();

        // Verify session was claimed
        Session updatedSession = sessionRepository.findById(guestSession.getId()).orElseThrow();
        assertThat(updatedSession.getUser().getId()).isEqualTo(newUser.getId());
        assertThat(updatedSession.getGuestId()).isNull();

        // Verify bookmark was claimed
        Bookmark updatedBookmark = bookmarkRepository.findById(guestBookmark.getId()).orElseThrow();
        assertThat(updatedBookmark.getUser().getId()).isEqualTo(newUser.getId());
        assertThat(updatedBookmark.getGuestId()).isNull();
    }

    @Test
    @DisplayName("POST /login with X-Guest-Id migrates guest sessions to existing account")
    void login_withXGuestId_migratesData() throws Exception {
        User user = new User("existinglogin@example.com", passwordEncoder.encode("Password123!"), "Existing", Role.ROLE_USER);
        user = userRepository.saveAndFlush(user);

        String guestId = "guest-login-test-uuid";

        Category category = categoryRepository.save(new Category("Cat2", "Desc"));
        Prompt prompt = new Prompt();
        prompt.setText("Prompt 2");
        prompt.setCategory(category);
        prompt.setMode(Mode.STORY);
        prompt = promptRepository.save(prompt);

        Session guestSession = new Session();
        guestSession.setPrompt(prompt);
        guestSession.setPromptText(prompt.getText());
        guestSession.setMode(Mode.STORY);
        guestSession.setDurationSeconds(120);
        guestSession.setActualDurationSeconds(110);
        guestSession.setGuestId(guestId);
        guestSession = sessionRepository.save(guestSession);

        LoginRequestDto request = new LoginRequestDto("existinglogin@example.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Guest-Id", guestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        Session updatedSession = sessionRepository.findById(guestSession.getId()).orElseThrow();
        assertThat(updatedSession.getUser().getId()).isEqualTo(user.getId());
        assertThat(updatedSession.getGuestId()).isNull();
    }
}
