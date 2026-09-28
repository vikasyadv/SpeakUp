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
public class RecentSessionActivityDto {
    private Long sessionId;
    private String promptText;
    private String mode;
    private Instant completedAt;
    private int durationSeconds;
    private Integer overallScore;
    private boolean hasFeedback;
    private Long promptId;

    public RecentSessionActivityDto(Long sessionId, String promptText, String mode,
                                    Instant completedAt, int durationSeconds,
                                    Integer overallScore, boolean hasFeedback) {
        this(sessionId, promptText, mode, completedAt, durationSeconds, overallScore, hasFeedback, null);
    }
}
