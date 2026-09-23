package com.speakup.service;

import com.speakup.dto.AuthResponseDto;
import com.speakup.dto.LoginRequestDto;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.dto.UpdateProfileRequestDto;
import com.speakup.dto.UserDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.Bookmark;
import com.speakup.model.Role;
import com.speakup.model.Session;
import com.speakup.model.User;
import com.speakup.repository.BookmarkRepository;
import com.speakup.repository.SessionRepository;
import com.speakup.repository.UserRepository;
import com.speakup.security.JwtTokenProvider;
import com.speakup.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final SessionRepository sessionRepository;
    private final BookmarkRepository bookmarkRepository;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuthenticationManager authenticationManager,
                       SessionRepository sessionRepository,
                       BookmarkRepository bookmarkRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.authenticationManager = authenticationManager;
        this.sessionRepository = sessionRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Transactional
    public AuthResponseDto register(RegisterRequestDto request, String guestId) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        String displayName = request.getDisplayName();
        if (displayName != null) {
            displayName = displayName.trim();
            if (displayName.isEmpty()) {
                displayName = null;
            }
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = new User(email, encodedPassword, displayName, Role.ROLE_USER);
        User savedUser = userRepository.save(user);

        if (guestId != null && !guestId.isBlank()) {
            migrateGuestData(savedUser, guestId.trim());
        }

        UserPrincipal principal = UserPrincipal.create(savedUser);
        String token = tokenProvider.generateToken(principal);

        log.info("User registered successfully: id={}, email={}", savedUser.getId(), savedUser.getEmail());
        return new AuthResponseDto(token, UserDto.from(savedUser));
    }

    @Transactional
    public AuthResponseDto login(LoginRequestDto request, String guestId) {
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (guestId != null && !guestId.isBlank()) {
            migrateGuestData(user, guestId.trim());
        }

        String token = tokenProvider.generateToken(principal);

        log.info("User logged in successfully: id={}, email={}", user.getId(), user.getEmail());
        return new AuthResponseDto(token, UserDto.from(user));
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(UserPrincipal principal) {
        if (principal == null || principal.getId() == null) {
            throw new BadCredentialsException("Unauthenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        return UserDto.from(user);
    }

    @Transactional
    public UserDto updateProfile(UserPrincipal principal, UpdateProfileRequestDto request) {
        if (principal == null || principal.getId() == null) {
            throw new BadCredentialsException("Unauthenticated");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        String trimmedDisplayName = request != null && request.getDisplayName() != null
                ? request.getDisplayName().trim()
                : "";

        if (trimmedDisplayName.isEmpty()) {
            throw new IllegalArgumentException("Display name cannot be blank");
        }
        if (trimmedDisplayName.length() > 50) {
            throw new IllegalArgumentException("Display name must be between 1 and 50 characters");
        }

        user.setDisplayName(trimmedDisplayName);
        User savedUser = userRepository.save(user);

        log.info("Profile updated successfully for userId={}: displayName='{}'", savedUser.getId(), savedUser.getDisplayName());
        return UserDto.from(savedUser);
    }

    @Transactional
    public void migrateGuestData(User user, String guestId) {
        if (guestId == null || guestId.isBlank() || user == null) {
            return;
        }

        String cleanGuestId = guestId.trim();

        // 1. Migrate unauthenticated guest sessions to this user
        List<Session> guestSessions = sessionRepository.findByGuestIdAndUserIsNull(cleanGuestId);
        if (!guestSessions.isEmpty()) {
            for (Session session : guestSessions) {
                session.setUser(user);
                session.setGuestId(null);
            }
            sessionRepository.saveAll(guestSessions);
            log.info("Migrated {} guest sessions for guestId={} to userId={}",
                    guestSessions.size(), cleanGuestId, user.getId());
        }

        // 2. Migrate unauthenticated guest bookmarks to this user (deduplicating prompt bookmarks)
        List<Bookmark> guestBookmarks = bookmarkRepository.findByGuestIdAndUserIsNull(cleanGuestId);
        if (!guestBookmarks.isEmpty()) {
            Set<Long> claimedPromptIds = new HashSet<>();
            for (Bookmark bookmark : guestBookmarks) {
                Long promptId = bookmark.getPrompt() != null ? bookmark.getPrompt().getId() : null;
                if (promptId != null) {
                    if (claimedPromptIds.contains(promptId) || bookmarkRepository.existsByPromptIdAndUser(promptId, user)) {
                        // User already bookmarked this prompt -> delete duplicate guest bookmark
                        bookmarkRepository.delete(bookmark);
                        continue;
                    }
                    claimedPromptIds.add(promptId);
                }
                bookmark.setUser(user);
                bookmark.setGuestId(null);
                bookmarkRepository.save(bookmark);
            }
            log.info("Processed {} guest bookmarks for guestId={} to userId={}",
                    guestBookmarks.size(), cleanGuestId, user.getId());
        }
    }
}
