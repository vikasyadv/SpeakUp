package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressSummaryDto {
    private int totalCompletedSessions;
    private int totalSpeakingTimeSeconds;
    private Double averageOverallScore;
    private int reviewedSessionsCount;
}
