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
public class ScoreHistoryPointDto {
    private Long sessionId;
    private Instant date;
    private String mode;
    private int overallScore;
    private int clarityScore;
    private int relevanceScore;
    private int structureScore;
    private String promptText;
}
