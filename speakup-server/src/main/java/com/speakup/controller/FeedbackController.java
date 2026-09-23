package com.speakup.controller;

import com.speakup.dto.FeedbackDto;
import com.speakup.security.CallerContext;
import com.speakup.security.UserPrincipal;
import com.speakup.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<FeedbackDto> generateOrGetFeedback(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        FeedbackDto feedback = feedbackService.generateOrGetFeedback(sessionId, caller);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping
    public ResponseEntity<FeedbackDto> getFeedback(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        CallerContext caller = CallerContext.resolve(principal, guestId);
        FeedbackDto feedback = feedbackService.getFeedbackBySessionId(sessionId, caller);
        return ResponseEntity.ok(feedback);
    }
}
