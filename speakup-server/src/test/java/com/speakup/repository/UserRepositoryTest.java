package com.speakup.repository;

import com.speakup.model.Role;
import com.speakup.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveUser_persistsEntityWithTimestampsAndDefaultRole() {
        User user = new User();
        user.setEmail("speaker@example.com");
        user.setPasswordHash("$2a$10$hashedpasswordstringforuser");
        user.setDisplayName("Jane Speaker");

        User saved = userRepository.saveAndFlush(user);

        assertNotNull(saved.getId());
        assertEquals("speaker@example.com", saved.getEmail());
        assertEquals("Jane Speaker", saved.getDisplayName());
        assertEquals(Role.ROLE_USER, saved.getRole());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void saveUser_normalizesEmailToLowercase() {
        User user = new User();
        user.setEmail("  SPEAKER.UPPER@EXAMPLE.COM  ");
        user.setPasswordHash("$2a$10$hashedpasswordstringforuser");

        User saved = userRepository.saveAndFlush(user);

        assertEquals("speaker.upper@example.com", saved.getEmail());
    }

    @Test
    void findByEmail_findsUserByLowercaseEmail() {
        User user = new User("orator@example.com", "$2a$10$hashedpass", "Orator", Role.ROLE_USER);
        userRepository.saveAndFlush(user);

        Optional<User> found = userRepository.findByEmail("orator@example.com");
        assertTrue(found.isPresent());
        assertEquals("Orator", found.get().getDisplayName());
    }

    @Test
    void findByEmail_returnsEmptyWhenNotFound() {
        Optional<User> found = userRepository.findByEmail("nonexistent@example.com");
        assertTrue(found.isEmpty());
    }

    @Test
    void existsByEmail_returnsCorrectBoolean() {
        User user = new User("exists@example.com", "$2a$10$hashedpass", "Exists", Role.ROLE_USER);
        userRepository.saveAndFlush(user);

        assertTrue(userRepository.existsByEmail("exists@example.com"));
        assertFalse(userRepository.existsByEmail("other@example.com"));
    }

    @Test
    void saveUser_duplicateEmail_throwsDataIntegrityViolationException() {
        User user1 = new User("unique@example.com", "$2a$10$pass1", "User 1", Role.ROLE_USER);
        userRepository.saveAndFlush(user1);

        User user2 = new User("unique@example.com", "$2a$10$pass2", "User 2", Role.ROLE_USER);
        assertThrows(DataIntegrityViolationException.class, () -> {
            userRepository.saveAndFlush(user2);
        });
    }
}
