package com.speakup.service.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiFeedbackResponse {
    private Integer overallScore;
    private Integer clarityScore;
    private Integer relevanceScore;
    private Integer structureScore;
    private String summary;
    private List<String> strengths;
    private List<String> improvements;
}
