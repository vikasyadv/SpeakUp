package com.speakup.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "feedback", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"session_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private Session session;

    @Column(name = "overall_score", nullable = false)
    private Integer overallScore;

    @Column(name = "clarity_score", nullable = false)
    private Integer clarityScore;

    @Column(name = "relevance_score", nullable = false)
    private Integer relevanceScore;

    @Column(name = "structure_score", nullable = false)
    private Integer structureScore;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String strengths;

    @Column(columnDefinition = "TEXT")
    private String improvements;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
