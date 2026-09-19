package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackDto {
    private Long id;
    private Long sessionId;
    private Integer overallScore;
    private Integer clarityScore;
    private Integer relevanceScore;
    private Integer structureScore;
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
    private Instant createdAt;
}
