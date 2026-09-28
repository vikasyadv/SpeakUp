package com.speakup.controller;

import com.speakup.dto.ProgressDashboardDto;
import com.speakup.security.CallerContext;
import com.speakup.security.UserPrincipal;
import com.speakup.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/progress")
    public ResponseEntity<ProgressDashboardDto> getProgress(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        ProgressDashboardDto dto = analyticsService.getProgressDashboard(caller);
        return ResponseEntity.ok(dto);
    }
}
