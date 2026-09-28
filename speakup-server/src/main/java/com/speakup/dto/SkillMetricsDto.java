package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SkillMetricsDto {
    private Double averageClarity;
    private Double averageRelevance;
    private Double averageStructure;
}
