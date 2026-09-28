package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ModeBreakdownDto {
    private int offTheCuffCount;
    private int researchCount;
    private int debateCount;
    private int storyCount;
}
