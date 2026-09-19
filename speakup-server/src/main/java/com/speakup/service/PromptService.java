package com.speakup.service;

import com.speakup.dto.CategoryDto;
import com.speakup.dto.PromptDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.mapper.PromptMapper;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PromptService {

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;

    public PromptService(PromptRepository promptRepository, CategoryRepository categoryRepository) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
    }

    /**
     * Get a random prompt, optionally filtered by category and excluding a specific prompt ID.
     */
    public PromptDto getRandomPrompt(Mode mode, String category, Long excludeId) {
        List<Prompt> prompts;

        if (category != null && excludeId != null) {
            prompts = promptRepository.findByModeAndCategoryNameExcluding(mode, category, excludeId);
        } else if (category != null) {
            prompts = promptRepository.findByModeAndCategoryName(mode, category);
        } else if (excludeId != null) {
            prompts = promptRepository.findByModeExcluding(mode, excludeId);
        } else {
            prompts = promptRepository.findByModeAndActiveTrue(mode);
        }

        if (prompts.isEmpty()) {
            throw new ResourceNotFoundException("No prompts found for mode: " + mode);
        }

        int randomIndex = ThreadLocalRandom.current().nextInt(prompts.size());
        return PromptMapper.toDto(prompts.get(randomIndex));
    }

    /**
     * Get a specific prompt by ID.
     */
    public PromptDto getById(Long id) {
        Prompt prompt = promptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prompt not found with id: " + id));
        return PromptMapper.toDto(prompt);
    }

    /**
     * Get all categories.
     */
    public List<CategoryDto> getCategories() {
        return categoryRepository.findAll().stream()
                .map(PromptMapper::toCategoryDto)
                .toList();
    }

    /**
     * Get categories that have active prompts for the specified mode.
     * If mode is null, falls back to returning all categories.
     */
    public List<CategoryDto> getCategories(Mode mode) {
        if (mode == null) {
            return getCategories();
        }
        return promptRepository.findDistinctCategoriesByMode(mode).stream()
                .map(PromptMapper::toCategoryDto)
                .toList();
    }
}
