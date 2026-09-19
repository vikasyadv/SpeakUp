package com.speakup.mapper;

import com.speakup.dto.SessionDto;
import com.speakup.model.Session;

public class SessionMapper {

    private SessionMapper() {
        // Utility class
    }

    public static SessionDto toDto(Session session) {
        return new SessionDto(
                session.getId(),
                session.getPromptText(),
                session.getPrompt() != null ? session.getPrompt().getId() : null,
                session.getMode().name(),
                session.getDurationSeconds(),
                session.getStatus().name(),
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt()
        );
    }
}
