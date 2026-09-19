package com.speakup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "sessions")
@Getter
@Setter
@NoArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The prompt text at the time of the session.
     * Stored as a snapshot so session history survives prompt changes.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String promptText;

    /**
     * Optional reference to the original prompt, if it still exists.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id")
    private Prompt prompt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode;

    @Column(nullable = false)
    private Integer durationSeconds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status = SessionStatus.IN_PROGRESS;

    @Column(name = "actual_duration_seconds")
    private Integer actualDurationSeconds;

    @Column(columnDefinition = "LONGTEXT")
    private String transcript;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }
}
