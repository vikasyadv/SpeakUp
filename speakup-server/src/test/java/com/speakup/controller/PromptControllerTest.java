package com.speakup.controller;

import com.speakup.dto.CategoryDto;
import com.speakup.dto.PromptDto;
import com.speakup.model.Mode;
import com.speakup.service.PromptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class PromptControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PromptService promptService;

    @InjectMocks
    private PromptController promptController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(promptController).build();
    }

    @Test
    void getRandomPrompt_researchMode_returnsResearchPrompt() throws Exception {
        PromptDto dto = new PromptDto(101L, "How will AI change education?", "Technology", "RESEARCH");
        when(promptService.getRandomPrompt(Mode.RESEARCH, null, null)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/prompts/random")
                        .param("mode", "RESEARCH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.mode").value("RESEARCH"))
                .andExpect(jsonPath("$.category").value("Technology"))
                .andExpect(jsonPath("$.text").value("How will AI change education?"));

        verify(promptService).getRandomPrompt(Mode.RESEARCH, null, null);
    }

    @Test
    void getRandomPrompt_withCategory_passesCategoryToService() throws Exception {
        PromptDto dto = new PromptDto(102L, "What are the barriers to fusion power?", "Science", "RESEARCH");
        when(promptService.getRandomPrompt(Mode.RESEARCH, "Science", null)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/prompts/random")
                        .param("mode", "RESEARCH")
                        .param("category", "Science")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Science"))
                .andExpect(jsonPath("$.mode").value("RESEARCH"));

        verify(promptService).getRandomPrompt(Mode.RESEARCH, "Science", null);
    }

    @Test
    void getRandomPrompt_withCategoryAndExcludeId_passesAllParamsToService() throws Exception {
        PromptDto dto = new PromptDto(103L, "How do catalysts work?", "Science", "RESEARCH");
        when(promptService.getRandomPrompt(Mode.RESEARCH, "Science", 102L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/prompts/random")
                        .param("mode", "RESEARCH")
                        .param("category", "Science")
                        .param("excludeId", "102")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(103))
                .andExpect(jsonPath("$.category").value("Science"));

        verify(promptService).getRandomPrompt(Mode.RESEARCH, "Science", 102L);
    }

    @Test
    void getRandomPrompt_defaultMode_isOffTheCuff() throws Exception {
        PromptDto dto = new PromptDto(1L, "Cybersecurity", "Technology", "OFF_THE_CUFF");
        when(promptService.getRandomPrompt(Mode.OFF_THE_CUFF, null, null)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/prompts/random")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("OFF_THE_CUFF"));

        verify(promptService).getRandomPrompt(Mode.OFF_THE_CUFF, null, null);
    }

    @Test
    void getCategories_researchMode_returnsResearchCategories() throws Exception {
        List<CategoryDto> categories = List.of(
                new CategoryDto(1L, "Technology", "Tech topics"),
                new CategoryDto(2L, "Ethics", "Ethical dilemmas")
        );
        when(promptService.getCategories(Mode.RESEARCH)).thenReturn(categories);

        mockMvc.perform(get("/api/v1/prompts/categories")
                        .param("mode", "RESEARCH")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Technology"))
                .andExpect(jsonPath("$[1].name").value("Ethics"));

        verify(promptService).getCategories(Mode.RESEARCH);
    }

    @Test
    void getCategories_offTheCuffMode_returnsOffTheCuffCategories() throws Exception {
        List<CategoryDto> categories = List.of(
                new CategoryDto(1L, "Technology", "Tech topics"),
                new CategoryDto(3L, "Mindset", "Mindset topics")
        );
        when(promptService.getCategories(Mode.OFF_THE_CUFF)).thenReturn(categories);

        mockMvc.perform(get("/api/v1/prompts/categories")
                        .param("mode", "OFF_THE_CUFF")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Technology"))
                .andExpect(jsonPath("$[1].name").value("Mindset"));

        verify(promptService).getCategories(Mode.OFF_THE_CUFF);
    }

    @Test
    void getCategories_noModeParam_defaultsToOffTheCuff() throws Exception {
        List<CategoryDto> categories = List.of(
                new CategoryDto(1L, "Technology", "Tech topics")
        );
        when(promptService.getCategories(Mode.OFF_THE_CUFF)).thenReturn(categories);

        mockMvc.perform(get("/api/v1/prompts/categories")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Technology"));

        verify(promptService).getCategories(Mode.OFF_THE_CUFF);
    }
}
