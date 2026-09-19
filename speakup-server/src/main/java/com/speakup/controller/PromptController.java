package com.speakup.controller;

import com.speakup.dto.CategoryDto;
import com.speakup.dto.PromptDto;
import com.speakup.model.Mode;
import com.speakup.service.PromptService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prompts")
public class PromptController {

    private final PromptService promptService;

    public PromptController(PromptService promptService) {
        this.promptService = promptService;
    }

    @GetMapping("/random")
    public ResponseEntity<PromptDto> getRandomPrompt(
            @RequestParam(defaultValue = "OFF_THE_CUFF") String mode,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long excludeId) {

        Mode modeEnum = Mode.valueOf(mode.toUpperCase());
        PromptDto prompt = promptService.getRandomPrompt(modeEnum, category, excludeId);
        return ResponseEntity.ok(prompt);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromptDto> getPromptById(@PathVariable Long id) {
        return ResponseEntity.ok(promptService.getById(id));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(required = false) String mode) {

        if (mode != null && !mode.isBlank()) {
            Mode modeEnum = Mode.valueOf(mode.toUpperCase());
            return ResponseEntity.ok(promptService.getCategories(modeEnum));
        }
        return ResponseEntity.ok(promptService.getCategories(Mode.OFF_THE_CUFF));
    }
}
