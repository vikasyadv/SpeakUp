package com.speakup.controller;

import com.speakup.dto.AuthResponseDto;
import com.speakup.dto.LoginRequestDto;
import com.speakup.dto.RegisterRequestDto;
import com.speakup.dto.UserDto;
import com.speakup.security.UserPrincipal;
import com.speakup.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(
            @Valid @RequestBody RegisterRequestDto request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        AuthResponseDto response = authService.register(request, guestId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(
            @Valid @RequestBody LoginRequestDto request,
            @RequestHeader(value = "X-Guest-Id", required = false) String guestId) {
        AuthResponseDto response = authService.login(request, guestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getCurrentUser(principal);
        return ResponseEntity.ok(user);
    }
}
