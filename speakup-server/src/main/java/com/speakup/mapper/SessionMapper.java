package com.speakup.mapper;

import com.speakup.dto.SessionDto;
import com.speakup.model.Session;

public class SessionMapper {

    private SessionMapper() {
        // Utility class
    }

    public static SessionDto toDto(Session session) {
        return toDto(session, false);
    }

    public static SessionDto toDto(Session session, boolean hasFeedback) {
        String category = null;
        if (session.getPrompt() != null && session.getPrompt().getCategory() != null) {
            category = session.getPrompt().getCategory().getName();
        }

        return new SessionDto(
                session.getId(),
                session.getPromptText(),
                session.getPrompt() != null ? session.getPrompt().getId() : null,
                category,
                session.getMode() != null ? session.getMode().name() : null,
                session.getDurationSeconds(),
                session.getActualDurationSeconds(),
                session.getTranscript(),
                session.getStatus() != null ? session.getStatus().name() : null,
                hasFeedback,
                session.getStartedAt(),
                session.getCompletedAt(),
                session.getCreatedAt(),
                session.getPreparationNotes(),
                session.getPreparationDurationSeconds()
        );
    }
}
