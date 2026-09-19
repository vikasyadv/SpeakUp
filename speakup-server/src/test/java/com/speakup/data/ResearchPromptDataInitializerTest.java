package com.speakup.data;

import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ResearchPromptDataInitializerTest {

    @Autowired
    private ResearchPromptDataInitializer initializer;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private com.speakup.service.PromptService promptService;

    @Test
    void testResearchSeedDataLoadsSuccessfully() {
        List<Prompt> researchPrompts = promptRepository.findByModeAndActiveTrue(Mode.RESEARCH);
        assertFalse(researchPrompts.isEmpty(), "Research prompts should be loaded on startup");
        assertEquals(1200, researchPrompts.size(), "All 1200 research prompts should be seeded");

        for (Prompt prompt : researchPrompts) {
            assertEquals(Mode.RESEARCH, prompt.getMode(), "Prompt mode must be RESEARCH");
            assertNotNull(prompt.getId(), "Prompt ID must be database-generated");
            assertNotNull(prompt.getText(), "Prompt text must not be null");
            assertNotNull(prompt.getCategory(), "Prompt category must not be null");
            assertTrue(prompt.getActive(), "Prompt must be active");
        }
    }

    @Test
    void testRepeatedInitializationDoesNotCreateDuplicates() {
        int initialCount = promptRepository.findByModeAndActiveTrue(Mode.RESEARCH).size();
        assertTrue(initialCount > 0, "Initial research prompt count should be greater than zero");

        // Run the initializer a second time
        int newlySeeded = initializer.initializeResearchPrompts();
        assertEquals(0, newlySeeded, "Repeated execution should not insert duplicate prompts");

        int finalCount = promptRepository.findByModeAndActiveTrue(Mode.RESEARCH).size();
        assertEquals(initialCount, finalCount, "Prompt count should remain identical after second run");
    }

    @Test
    void testOffTheCuffPromptsRemainUnaffected() {
        Category techCategory = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(new Category("Technology", "Tech topics")));

        // Insert an OFF_THE_CUFF prompt
        Prompt otcPrompt = new Prompt();
        otcPrompt.setText("Quantum Computing");
        otcPrompt.setMode(Mode.OFF_THE_CUFF);
        otcPrompt.setCategory(techCategory);
        otcPrompt.setActive(true);
        Prompt savedOtc = promptRepository.save(otcPrompt);
        assertNotNull(savedOtc.getId());

        int researchCountBefore = promptRepository.findByModeAndActiveTrue(Mode.RESEARCH).size();

        // Run initializer again
        initializer.initializeResearchPrompts();

        // Verify OFF_THE_CUFF prompt still exists and is untouched
        Prompt retrievedOtc = promptRepository.findById(savedOtc.getId()).orElse(null);
        assertNotNull(retrievedOtc);
        assertEquals("Quantum Computing", retrievedOtc.getText());
        assertEquals(Mode.OFF_THE_CUFF, retrievedOtc.getMode());

        // Verify mode isolation: findByModeAndActiveTrue(OFF_THE_CUFF) only returns OFF_THE_CUFF
        List<Prompt> otcList = promptRepository.findByModeAndActiveTrue(Mode.OFF_THE_CUFF);
        assertTrue(otcList.stream().allMatch(p -> p.getMode() == Mode.OFF_THE_CUFF));

        // Verify research prompt count was unaffected
        int researchCountAfter = promptRepository.findByModeAndActiveTrue(Mode.RESEARCH).size();
        assertEquals(researchCountBefore, researchCountAfter);
    }

    @Test
    void testDistinctCategoriesForResearchModeReturnsAll12CategoriesInOrder() {
        var categories = promptService.getCategories(Mode.RESEARCH);
        assertEquals(12, categories.size(), "Should return exactly 12 distinct research categories");

        List<String> categoryNames = categories.stream().map(com.speakup.dto.CategoryDto::getName).toList();
        List<String> expectedSorted = List.of(
                "Business & Economy",
                "Culture",
                "Education",
                "Environment",
                "Ethics",
                "Future",
                "Health & Lifestyle",
                "Politics & Civics",
                "Psychology",
                "Science",
                "Society",
                "Technology"
        );
        assertEquals(expectedSorted, categoryNames, "Categories must be in alphabetical order");
    }

    @Test
    void testRandomPromptIntegrationWithCategoryAndExcludeId() {
        // Test random prompt for Mode.RESEARCH
        var prompt = promptService.getRandomPrompt(Mode.RESEARCH, null, null);
        assertNotNull(prompt);
        assertEquals("RESEARCH", prompt.getMode());

        // Test random prompt filtered by category
        var techPrompt = promptService.getRandomPrompt(Mode.RESEARCH, "Technology", null);
        assertNotNull(techPrompt);
        assertEquals("Technology", techPrompt.getCategory());
        assertEquals("RESEARCH", techPrompt.getMode());

        // Test exclusion prevents immediate repetition
        Long excludedId = techPrompt.getId();
        var nextPrompt = promptService.getRandomPrompt(Mode.RESEARCH, "Technology", excludedId);
        assertNotNull(nextPrompt);
        assertNotEquals(excludedId, nextPrompt.getId());
        assertEquals("Technology", nextPrompt.getCategory());
    }
}
