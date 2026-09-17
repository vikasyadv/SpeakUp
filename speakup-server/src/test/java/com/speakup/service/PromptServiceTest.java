package com.speakup.service;

import com.speakup.dto.PromptDto;
import com.speakup.exception.ResourceNotFoundException;
import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromptServiceTest {

    @Mock
    private PromptRepository promptRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private PromptService promptService;

    private Prompt samplePrompt;
    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new Category("Technology", "Tech topics");
        sampleCategory.setId(1L);

        samplePrompt = new Prompt();
        samplePrompt.setId(1L);
        samplePrompt.setText("Should AI be regulated?");
        samplePrompt.setCategory(sampleCategory);
        samplePrompt.setMode(Mode.OFF_THE_CUFF);
        samplePrompt.setActive(true);
    }

    @Test
    void getRandomPrompt_returnsPrompt_whenPromptsExist() {
        when(promptRepository.findByModeAndActiveTrue(Mode.OFF_THE_CUFF))
                .thenReturn(List.of(samplePrompt));

        PromptDto result = promptService.getRandomPrompt(Mode.OFF_THE_CUFF, null, null);

        assertNotNull(result);
        assertEquals("Should AI be regulated?", result.getText());
        assertEquals("Technology", result.getCategory());
    }

    @Test
    void getRandomPrompt_throwsException_whenNoPromptsFound() {
        when(promptRepository.findByModeAndActiveTrue(Mode.OFF_THE_CUFF))
                .thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> promptService.getRandomPrompt(Mode.OFF_THE_CUFF, null, null));
    }

    @Test
    void getById_returnsPrompt_whenExists() {
        when(promptRepository.findById(1L)).thenReturn(Optional.of(samplePrompt));

        PromptDto result = promptService.getById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_throwsException_whenNotFound() {
        when(promptRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> promptService.getById(99L));
    }
}
