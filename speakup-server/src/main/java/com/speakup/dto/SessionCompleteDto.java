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
    private String stance;

    public SessionCompleteDto(String transcript, Integer actualDurationSeconds) {
        this.transcript = transcript;
        this.actualDurationSeconds = actualDurationSeconds;
    }

    public SessionCompleteDto(String transcript, Integer actualDurationSeconds, String preparationNotes, Integer preparationDurationSeconds) {
        this.transcript = transcript;
        this.actualDurationSeconds = actualDurationSeconds;
        this.preparationNotes = preparationNotes;
        this.preparationDurationSeconds = preparationDurationSeconds;
    }
}
