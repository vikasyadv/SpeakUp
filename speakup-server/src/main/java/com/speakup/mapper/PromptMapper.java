package com.speakup.mapper;

import com.speakup.dto.CategoryDto;
import com.speakup.dto.PromptDto;
import com.speakup.model.Category;
import com.speakup.model.Prompt;

public class PromptMapper {

    private PromptMapper() {
        // Utility class
    }

    public static PromptDto toDto(Prompt prompt) {
        return new PromptDto(
                prompt.getId(),
                prompt.getText(),
                prompt.getCategory().getName(),
                prompt.getMode().name()
        );
    }

    public static CategoryDto toCategoryDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
