package com.speakup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessionCreateDto {

    @NotBlank(message = "Prompt text is required")
    private String promptText;

    private Long promptId;

    @NotBlank(message = "Mode is required")
    private String mode;

    @NotNull(message = "Duration is required")
    @Positive(message = "Duration must be positive")
    private Integer durationSeconds;

    private String stance;

    public SessionCreateDto(String promptText, Long promptId, String mode, Integer durationSeconds) {
        this.promptText = promptText;
        this.promptId = promptId;
        this.mode = mode;
        this.durationSeconds = durationSeconds;
    }
}
