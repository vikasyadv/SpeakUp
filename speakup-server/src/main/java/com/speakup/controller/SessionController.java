package com.speakup.controller;

import com.speakup.dto.SessionCompleteDto;
import com.speakup.dto.SessionCreateDto;
import com.speakup.dto.SessionDto;
import com.speakup.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    public ResponseEntity<SessionDto> createSession(@Valid @RequestBody SessionCreateDto dto) {
        SessionDto session = sessionService.createSession(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<SessionDto> completeSession(
            @PathVariable Long id,
            @RequestBody(required = false) SessionCompleteDto dto) {
        return ResponseEntity.ok(sessionService.completeSession(id, dto));
    }

    @PatchMapping("/{id}/abandon")
    public ResponseEntity<SessionDto> abandonSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.abandonSession(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SessionDto> getSession(@PathVariable Long id) {
        return ResponseEntity.ok(sessionService.getById(id));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<SessionDto>> getRecentSessions() {
        return ResponseEntity.ok(sessionService.getRecentSessions());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSession(@PathVariable Long id) {
        sessionService.deleteSession(id);
        return ResponseEntity.noContent().build();
    }
}
