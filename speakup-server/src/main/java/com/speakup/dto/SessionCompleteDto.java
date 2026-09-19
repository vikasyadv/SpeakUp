package com.speakup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCompleteDto {
    private String transcript;
    private Integer actualDurationSeconds;
    private String preparationNotes;
    private Integer preparationDurationSeconds;

    public SessionCompleteDto(String transcript, Integer actualDurationSeconds) {
        this.transcript = transcript;
        this.actualDurationSeconds = actualDurationSeconds;
    }
}
