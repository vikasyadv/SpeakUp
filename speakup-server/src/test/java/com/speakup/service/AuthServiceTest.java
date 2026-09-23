package com.speakup.service;

import com.speakup.dto.AuthResponseDto;
import com.speakup.dto.LoginRequestDto;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.dto.UserDto;
import com.speakup.model.Bookmark;
import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.model.Role;
import com.speakup.model.Session;
import com.speakup.model.User;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.JwtTokenProvider;
import com.speakup.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository,
                passwordEncoder,
                tokenProvider,
                authenticationManager,
                sessionRepository,
                bookmarkRepository
        );
    }

    @Test
    @DisplayName("register creates user with hashed password, saves user, generates token, and returns response")
    void register_success() {
        RegisterRequestDto request = new RegisterRequestDto("alice@example.com", "Password123!", "Alice Wonder");

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashedPassword");

        User savedUser = new User("alice@example.com", "$2a$10$hashedPassword", "Alice Wonder", Role.ROLE_USER);
        savedUser.setId(1L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token-123");

        AuthResponseDto response = authService.register(request, null);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getUser().getDisplayName()).isEqualTo("Alice Wonder");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User captured = captor.getValue();
        assertThat(captured.getEmail()).isEqualTo("alice@example.com");
        assertThat(captured.getPasswordHash()).isEqualTo("$2a$10$hashedPassword");
        assertThat(captured.getDisplayName()).isEqualTo("Alice Wonder");
        assertThat(captured.getRole()).isEqualTo(Role.ROLE_USER);
    }

    @Test
    @DisplayName("register throws IllegalArgumentException when email already exists")
    void register_duplicateEmail_throwsIllegalArgumentException() {
        RegisterRequestDto request = new RegisterRequestDto("existing@example.com", "Password123!", "Existing User");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered");

        verify(userRepository, never()).save(any());
        verify(tokenProvider, never()).generateToken(any(UserPrincipal.class));
    }

    @Test
    @DisplayName("register migrates guest sessions and bookmarks when guestId is provided")
    void register_withGuestId_migratesData() {
        RegisterRequestDto request = new RegisterRequestDto("migrating@example.com", "Password123!", "Migrator");

        when(userRepository.existsByEmail("migrating@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$hashed");

        User savedUser = new User("migrating@example.com", "$2a$10$hashed", "Migrator", Role.ROLE_USER);
        savedUser.setId(2L);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(any(UserPrincipal.class))).thenReturn("jwt-token-migrated");

        // Prepare guest sessions
        Session guestSession = new Session();
        guestSession.setId(10L);
        guestSession.setGuestId("guest-uuid-1");
        when(sessionRepository.findByGuestIdAndUserIsNull("guest-uuid-1")).thenReturn(new ArrayList<>(List.of(guestSession)));

        // Prepare guest bookmarks
        Prompt prompt = new Prompt();
        prompt.setId(100L);
        Bookmark guestBookmark = new Bookmark();
        guestBookmark.setId(20L);
        guestBookmark.setGuestId("guest-uuid-1");
        guestBookmark.setPrompt(prompt);
        when(bookmarkRepository.findByGuestIdAndUserIsNull("guest-uuid-1")).thenReturn(new ArrayList<>(List.of(guestBookmark)));
        when(bookmarkRepository.existsByPromptIdAndUser(100L, savedUser)).thenReturn(false);

        AuthResponseDto response = authService.register(request, "guest-uuid-1");

        assertThat(response.getToken()).isEqualTo("jwt-token-migrated");

        // Verify session migrated
        assertThat(guestSession.getUser()).isEqualTo(savedUser);
        assertThat(guestSession.getGuestId()).isNull();
        verify(sessionRepository).saveAll(any());

        // Verify bookmark migrated
        assertThat(guestBookmark.getUser()).isEqualTo(savedUser);
        assertThat(guestBookmark.getGuestId()).isNull();
        verify(bookmarkRepository).save(guestBookmark);
    }

    @Test
    @DisplayName("login authenticates credentials, generates token, and returns UserDto")
    void login_success() {
        LoginRequestDto request = new LoginRequestDto("bob@example.com", "Password123!");

        User user = new User("bob@example.com", "$2a$10$hashed", "Bob", Role.ROLE_USER);
        user.setId(5L);
        UserPrincipal principal = UserPrincipal.create(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(principal)).thenReturn("jwt-token-bob");

        AuthResponseDto response = authService.login(request, null);

        assertThat(response.getToken()).isEqualTo("jwt-token-bob");
        assertThat(response.getUser().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("login throws BadCredentialsException on authentication failure")
    void login_invalidCredentials_throwsBadCredentialsException() {
        LoginRequestDto request = new LoginRequestDto("bob@example.com", "WrongPassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request, null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Bad credentials");

        verify(tokenProvider, never()).generateToken(any(UserPrincipal.class));
    }

    @Test
    @DisplayName("getCurrentUser returns UserDto for authenticated principal")
    void getCurrentUser_success() {
        User user = new User("carol@example.com", "$2a$10$hashed", "Carol", Role.ROLE_USER);
        user.setId(7L);
        UserPrincipal principal = UserPrincipal.create(user);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserDto dto = authService.getCurrentUser(principal);

        assertThat(dto).isNotNull();
        assertThat(dto.getEmail()).isEqualTo("carol@example.com");
        assertThat(dto.getDisplayName()).isEqualTo("Carol");
    }

    @Test
    @DisplayName("getCurrentUser throws BadCredentialsException when principal is null")
    void getCurrentUser_nullPrincipal_throwsBadCredentialsException() {
        assertThatThrownBy(() -> authService.getCurrentUser(null))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Unauthenticated");
    }

    @Test
    @DisplayName("migrateGuestData removes duplicate bookmarks if user already has bookmarked prompt")
    void migrateGuestData_deduplicatesBookmarks() {
        User user = new User("user@example.com", "hash", "User", Role.ROLE_USER);
        user.setId(3L);

        Prompt promptAlreadyBookmarked = new Prompt();
        promptAlreadyBookmarked.setId(50L);
        Bookmark duplicateGuestBookmark = new Bookmark();
        duplicateGuestBookmark.setId(101L);
        duplicateGuestBookmark.setPrompt(promptAlreadyBookmarked);

        Prompt promptNew = new Prompt();
        promptNew.setId(51L);
        Bookmark newGuestBookmark = new Bookmark();
        newGuestBookmark.setId(102L);
        newGuestBookmark.setPrompt(promptNew);

        when(sessionRepository.findByGuestIdAndUserIsNull("guest-123")).thenReturn(List.of());
        when(bookmarkRepository.findByGuestIdAndUserIsNull("guest-123"))
                .thenReturn(new ArrayList<>(List.of(duplicateGuestBookmark, newGuestBookmark)));

        // User already bookmarked prompt 50, but not prompt 51
        when(bookmarkRepository.existsByPromptIdAndUser(50L, user)).thenReturn(true);
        when(bookmarkRepository.existsByPromptIdAndUser(51L, user)).thenReturn(false);

        authService.migrateGuestData(user, "guest-123");

        // Duplicate deleted
        verify(bookmarkRepository).delete(duplicateGuestBookmark);
        // New bookmark saved with user
        verify(bookmarkRepository).save(newGuestBookmark);
        assertThat(newGuestBookmark.getUser()).isEqualTo(user);
        assertThat(newGuestBookmark.getGuestId()).isNull();
    }
}
