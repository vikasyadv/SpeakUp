package com.speakup.data;

import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import com.speakup.service.PromptService;
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
class DebatePromptDataInitializerTest {

    @Autowired
    private DebatePromptDataInitializer initializer;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PromptService promptService;

    @Test
    void testDebateSeedDataLoadsSuccessfully() {
        List<Prompt> debatePrompts = promptRepository.findByModeAndActiveTrue(Mode.DEBATE);
        assertFalse(debatePrompts.isEmpty(), "Debate prompts should be loaded on startup");
        assertEquals(24, debatePrompts.size(), "All 24 initial debate motions should be seeded");

        for (Prompt prompt : debatePrompts) {
            assertEquals(Mode.DEBATE, prompt.getMode(), "Prompt mode must be DEBATE");
            assertNotNull(prompt.getId(), "Prompt ID must be database-generated");
            assertNotNull(prompt.getText(), "Prompt text must not be null");
            assertNotNull(prompt.getCategory(), "Prompt category must not be null");
            assertTrue(prompt.getActive(), "Prompt must be active");
        }
    }

    @Test
    void testRepeatedInitializationDoesNotCreateDuplicates() {
        int initialCount = promptRepository.findByModeAndActiveTrue(Mode.DEBATE).size();
        assertTrue(initialCount > 0, "Initial debate prompt count should be greater than zero");

        // Run the initializer a second time
        int newlySeeded = initializer.initializeDebatePrompts();
        assertEquals(0, newlySeeded, "Repeated execution should not insert duplicate prompts");

        int finalCount = promptRepository.findByModeAndActiveTrue(Mode.DEBATE).size();
        assertEquals(initialCount, finalCount, "Prompt count should remain identical after second run");
    }

    @Test
    void testOtherModesRemainUnaffected() {
        Category techCategory = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(new Category("Technology", "Tech topics")));

        // Insert an OFF_THE_CUFF prompt
        Prompt otcPrompt = new Prompt();
        otcPrompt.setText("Impromptu debate topic");
        otcPrompt.setMode(Mode.OFF_THE_CUFF);
        otcPrompt.setCategory(techCategory);
        otcPrompt.setActive(true);
        Prompt savedOtc = promptRepository.save(otcPrompt);
        assertNotNull(savedOtc.getId());

        int debateCountBefore = promptRepository.findByModeAndActiveTrue(Mode.DEBATE).size();

        // Run initializer again
        initializer.initializeDebatePrompts();

        // Verify OFF_THE_CUFF prompt still exists and is untouched
        Prompt retrievedOtc = promptRepository.findById(savedOtc.getId()).orElse(null);
        assertNotNull(retrievedOtc);
        assertEquals("Impromptu debate topic", retrievedOtc.getText());
        assertEquals(Mode.OFF_THE_CUFF, retrievedOtc.getMode());

        // Verify mode isolation: findByModeAndActiveTrue(DEBATE) only returns DEBATE
        List<Prompt> debateList = promptRepository.findByModeAndActiveTrue(Mode.DEBATE);
        assertTrue(debateList.stream().allMatch(p -> p.getMode() == Mode.DEBATE));

        // Verify debate prompt count was unaffected
        int debateCountAfter = promptRepository.findByModeAndActiveTrue(Mode.DEBATE).size();
        assertEquals(debateCountBefore, debateCountAfter);
    }

    @Test
    void testDebateCategoriesAndRandomPrompt() {
        var categories = promptService.getCategories(Mode.DEBATE);
        assertFalse(categories.isEmpty(), "Debate categories should not be empty");

        var prompt = promptService.getRandomPrompt(Mode.DEBATE, null, null);
        assertNotNull(prompt);
        assertEquals("DEBATE", prompt.getMode());

        // Test category filtering for debate
        var techPrompt = promptService.getRandomPrompt(Mode.DEBATE, "Technology", null);
        assertNotNull(techPrompt);
        assertEquals("Technology", techPrompt.getCategory());
        assertEquals("DEBATE", techPrompt.getMode());

        // Test excludeId
        Long excludedId = techPrompt.getId();
        var nextPrompt = promptService.getRandomPrompt(Mode.DEBATE, "Technology", excludedId);
        assertNotNull(nextPrompt);
        assertNotEquals(excludedId, nextPrompt.getId());
    }
}
