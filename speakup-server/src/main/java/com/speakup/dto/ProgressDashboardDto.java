package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProgressDashboardDto {
    private ProgressSummaryDto summary;
    private SkillMetricsDto skills;
    private ModeBreakdownDto modeBreakdown;
    private List<ScoreHistoryPointDto> scoreHistory;
    private List<RecentSessionActivityDto> recentActivity;

    public static ProgressDashboardDto empty() {
        return new ProgressDashboardDto(
                new ProgressSummaryDto(0, 0, null, 0),
                new SkillMetricsDto(null, null, null),
                new ModeBreakdownDto(0, 0, 0, 0),
                List.of(),
                List.of()
        );
    }
}
