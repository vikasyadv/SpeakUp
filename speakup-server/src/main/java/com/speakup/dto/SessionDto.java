package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionDto {
    private Long id;
    private String promptText;
    private Long promptId;
    private String category;
    private String mode;
    private Integer durationSeconds;
    private Integer actualDurationSeconds;
    private String transcript;
    private String status;
    private Boolean hasFeedback;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
    private String preparationNotes;
    private Integer preparationDurationSeconds;
    private String stance;

    public SessionDto(Long id, String promptText, Long promptId, String category, String mode,
                      Integer durationSeconds, Integer actualDurationSeconds, String transcript,
                      String status, Boolean hasFeedback, Instant startedAt, Instant completedAt,
                      Instant createdAt, String preparationNotes, Integer preparationDurationSeconds) {
        this(id, promptText, promptId, category, mode, durationSeconds, actualDurationSeconds,
                transcript, status, hasFeedback, startedAt, completedAt, createdAt,
                preparationNotes, preparationDurationSeconds, null);
    }
}
