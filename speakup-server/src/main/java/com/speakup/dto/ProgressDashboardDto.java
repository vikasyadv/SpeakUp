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
    private int activeDaysLast7;
    private int activeDaysLast14;

    public ProgressDashboardDto(ProgressSummaryDto summary,
                                SkillMetricsDto skills,
                                ModeBreakdownDto modeBreakdown,
                                List<ScoreHistoryPointDto> scoreHistory,
                                List<RecentSessionActivityDto> recentActivity) {
        this(summary, skills, modeBreakdown, scoreHistory, recentActivity, 0, 0);
    }

    public static ProgressDashboardDto empty() {
        return new ProgressDashboardDto(
                new ProgressSummaryDto(0, 0, null, 0),
                new SkillMetricsDto(null, null, null),
                new ModeBreakdownDto(0, 0, 0, 0),
                List.of(),
                List.of(),
                0,
                0
        );
    }
}
