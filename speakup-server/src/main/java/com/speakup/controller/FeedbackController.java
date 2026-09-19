package com.speakup.controller;

import com.speakup.dto.FeedbackDto;
import com.speakup.service.FeedbackService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions/{sessionId}/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<FeedbackDto> generateOrGetFeedback(@PathVariable Long sessionId) {
        FeedbackDto feedback = feedbackService.generateOrGetFeedback(sessionId);
        return ResponseEntity.ok(feedback);
    }

    @GetMapping
    public ResponseEntity<FeedbackDto> getFeedback(@PathVariable Long sessionId) {
        FeedbackDto feedback = feedbackService.getFeedbackBySessionId(sessionId);
        return ResponseEntity.ok(feedback);
    }
}
