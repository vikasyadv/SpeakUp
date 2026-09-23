package com.speakup.security;

import com.speakup.model.Role;
import com.speakup.model.User;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void healthEndpoint_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void promptsRandomEndpoint_isPubliclyAccessibleWithoutToken() throws Exception {
        com.speakup.model.Category category = categoryRepository.save(new com.speakup.model.Category("General", "General topics"));
        com.speakup.model.Prompt prompt = new com.speakup.model.Prompt();
        prompt.setText("Public prompt test");
        prompt.setCategory(category);
        prompt.setMode(com.speakup.model.Mode.OFF_THE_CUFF);
        promptRepository.save(prompt);

        mockMvc.perform(get("/api/v1/prompts/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Public prompt test"));
    }

    @Test
    void promptsCategoriesEndpoint_isPubliclyAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/prompts/categories"))
                .andExpect(status().isOk());
    }

    @Test
    void sessionsRecentEndpoint_isPubliclyAccessibleForGuests() throws Exception {
        mockMvc.perform(get("/api/v1/sessions/recent"))
                .andExpect(status().isOk());
    }

    @Test
    void bookmarksEndpoint_isPubliclyAccessibleForGuests() throws Exception {
        mockMvc.perform(get("/api/v1/bookmarks"))
                .andExpect(status().isOk());
    }

    @Test
    void validBearerToken_isAcceptedOnPublicEndpointsWithoutError() throws Exception {
        User user = new User("valid.user@example.com", "$2a$10$hashedpass", "Valid User", Role.ROLE_USER);
        user = userRepository.saveAndFlush(user);

        String token = tokenProvider.generateToken(user.getEmail(), user.getId(), user.getRole().name());

        mockMvc.perform(get("/api/v1/health")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void invalidBearerToken_fallsBackToAnonymousWithoutBreakingPublicEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                        .header("Authorization", "Bearer invalid-tampered-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
