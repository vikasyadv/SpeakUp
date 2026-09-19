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

    @Test
    void getRandomPrompt_researchMode_returnsPromptWithResearchMode() {
        Prompt researchPrompt = new Prompt();
        researchPrompt.setId(201L);
        researchPrompt.setText("How will artificial intelligence transform higher education?");
        researchPrompt.setCategory(sampleCategory);
        researchPrompt.setMode(Mode.RESEARCH);
        researchPrompt.setActive(true);

        when(promptRepository.findByModeAndActiveTrue(Mode.RESEARCH))
                .thenReturn(List.of(researchPrompt));

        PromptDto result = promptService.getRandomPrompt(Mode.RESEARCH, null, null);

        assertNotNull(result);
        assertEquals(201L, result.getId());
        assertEquals("RESEARCH", result.getMode());
        assertEquals("Technology", result.getCategory());
        assertEquals("How will artificial intelligence transform higher education?", result.getText());
    }

    @Test
    void getRandomPrompt_withCategory_returnsOnlyRequestedCategory() {
        Prompt researchPrompt = new Prompt();
        researchPrompt.setId(202L);
        researchPrompt.setText("What are the primary technical barriers to fault-tolerant quantum computers?");
        researchPrompt.setCategory(sampleCategory);
        researchPrompt.setMode(Mode.RESEARCH);
        researchPrompt.setActive(true);

        when(promptRepository.findByModeAndCategoryName(Mode.RESEARCH, "Technology"))
                .thenReturn(List.of(researchPrompt));

        PromptDto result = promptService.getRandomPrompt(Mode.RESEARCH, "Technology", null);

        assertNotNull(result);
        assertEquals("Technology", result.getCategory());
        assertEquals("RESEARCH", result.getMode());
        verify(promptRepository).findByModeAndCategoryName(Mode.RESEARCH, "Technology");
    }

    @Test
    void getRandomPrompt_withCategoryAndExcludeId_preventsImmediateRepetition() {
        Prompt nextPrompt = new Prompt();
        nextPrompt.setId(203L);
        nextPrompt.setText("How can open-source software communities ensure cybersecurity standards?");
        nextPrompt.setCategory(sampleCategory);
        nextPrompt.setMode(Mode.RESEARCH);
        nextPrompt.setActive(true);

        when(promptRepository.findByModeAndCategoryNameExcluding(Mode.RESEARCH, "Technology", 202L))
                .thenReturn(List.of(nextPrompt));

        PromptDto result = promptService.getRandomPrompt(Mode.RESEARCH, "Technology", 202L);

        assertNotNull(result);
        assertEquals(203L, result.getId());
        assertNotEquals(202L, result.getId());
        verify(promptRepository).findByModeAndCategoryNameExcluding(Mode.RESEARCH, "Technology", 202L);
    }

    @Test
    void getRandomPrompt_withExcludeIdOnly_preventsImmediateRepetition() {
        Prompt nextPrompt = new Prompt();
        nextPrompt.setId(204L);
        nextPrompt.setText("How does semiconductor manufacturing concentration affect global resilience?");
        nextPrompt.setCategory(sampleCategory);
        nextPrompt.setMode(Mode.RESEARCH);
        nextPrompt.setActive(true);

        when(promptRepository.findByModeExcluding(Mode.RESEARCH, 201L))
                .thenReturn(List.of(nextPrompt));

        PromptDto result = promptService.getRandomPrompt(Mode.RESEARCH, null, 201L);

        assertNotNull(result);
        assertEquals(204L, result.getId());
        verify(promptRepository).findByModeExcluding(Mode.RESEARCH, 201L);
    }

    @Test
    void getCategories_byMode_returnsCategoriesForResearch() {
        Category scienceCategory = new Category("Science", "Scientific discovery");
        scienceCategory.setId(2L);

        when(promptRepository.findDistinctCategoriesByMode(Mode.RESEARCH))
                .thenReturn(List.of(sampleCategory, scienceCategory));

        var categories = promptService.getCategories(Mode.RESEARCH);

        assertEquals(2, categories.size());
        assertEquals("Technology", categories.get(0).getName());
        assertEquals("Science", categories.get(1).getName());
        verify(promptRepository).findDistinctCategoriesByMode(Mode.RESEARCH);
    }

    @Test
    void getCategories_byMode_returnsCategoriesForOffTheCuff() {
        when(promptRepository.findDistinctCategoriesByMode(Mode.OFF_THE_CUFF))
                .thenReturn(List.of(sampleCategory));

        var categories = promptService.getCategories(Mode.OFF_THE_CUFF);

        assertEquals(1, categories.size());
        assertEquals("Technology", categories.get(0).getName());
        verify(promptRepository).findDistinctCategoriesByMode(Mode.OFF_THE_CUFF);
    }

    @Test
    void getCategories_nullMode_returnsAllCategories() {
        when(categoryRepository.findAll()).thenReturn(List.of(sampleCategory));

        var categories = promptService.getCategories((Mode) null);

        assertEquals(1, categories.size());
        assertEquals("Technology", categories.get(0).getName());
        verify(categoryRepository).findAll();
    }
}
