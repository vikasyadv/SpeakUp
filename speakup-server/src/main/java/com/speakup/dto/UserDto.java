package com.speakup.dto;

import com.speakup.model.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    private Long id;
    private String email;
    private String displayName;
    private String role;
    private Instant createdAt;

    public static UserDto from(User user) {
        if (user == null) {
            return null;
        }
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole() != null ? user.getRole().name() : "ROLE_USER",
                user.getCreatedAt()
        );
    }
}
