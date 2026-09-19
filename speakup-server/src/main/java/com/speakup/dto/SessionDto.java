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
public class SessionDto {
    private Long id;
    private String promptText;
    private Long promptId;
    private String mode;
    private Integer durationSeconds;
    private String status;
    private Instant startedAt;
    private Instant completedAt;
    private Instant createdAt;
}
