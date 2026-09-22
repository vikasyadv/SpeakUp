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
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StoryPromptDataInitializerTest {

    @Autowired
    private StoryPromptDataInitializer initializer;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private PromptService promptService;

    private static final Set<String> EXPECTED_CATEGORIES = Set.of(
            "Adventure & Survival",
            "Mystery & Suspense",
            "Sci-Fi & Speculative",
            "Humorous & Everyday",
            "Moral Dilemmas & Drama",
            "Personal Anecdotes"
    );

    @Test
    void testStorySeedDataLoadsSuccessfully() {
        List<Prompt> storyPrompts = promptRepository.findByModeAndActiveTrue(Mode.STORY);
        assertFalse(storyPrompts.isEmpty(), "Story prompts should be loaded on startup");
        assertEquals(24, storyPrompts.size(), "Exactly 24 Story prompts must exist after initialization");

        for (Prompt prompt : storyPrompts) {
            assertEquals(Mode.STORY, prompt.getMode(), "Prompt mode must be STORY");
            assertNotNull(prompt.getId(), "Prompt ID must be database-generated");
            assertNotNull(prompt.getText(), "Prompt text must not be null");
            assertFalse(prompt.getText().isBlank(), "Prompt text must not be blank");
            assertNotNull(prompt.getCategory(), "Prompt category must not be null");
            assertTrue(prompt.getActive(), "Prompt must be active");
        }

        Set<String> presentCategories = storyPrompts.stream()
                .map(p -> p.getCategory().getName())
                .collect(Collectors.toSet());
        assertEquals(EXPECTED_CATEGORIES, presentCategories, "All six Story categories must exist");
    }

    @Test
    void testRepeatedInitializationDoesNotCreateDuplicates() {
        int initialCount = promptRepository.findByModeAndActiveTrue(Mode.STORY).size();
        assertEquals(24, initialCount, "Initial story prompt count should be 24");

        // Run the initializer a second time
        int newlySeeded = initializer.initializeStoryPrompts();
        assertEquals(0, newlySeeded, "Repeated execution should not insert duplicate prompts");

        int finalCount = promptRepository.findByModeAndActiveTrue(Mode.STORY).size();
        assertEquals(initialCount, finalCount, "Prompt count should remain identical after second run");
    }

    @Test
    void testOtherModesRemainUnaffected() {
        Category techCategory = categoryRepository.findByName("Technology")
                .orElseGet(() -> categoryRepository.save(new Category("Technology", "Tech topics")));

        // Insert an OFF_THE_CUFF prompt
        Prompt otcPrompt = new Prompt();
        otcPrompt.setText("Impromptu speech topic");
        otcPrompt.setMode(Mode.OFF_THE_CUFF);
        otcPrompt.setCategory(techCategory);
        otcPrompt.setActive(true);
        Prompt savedOtc = promptRepository.save(otcPrompt);
        assertNotNull(savedOtc.getId());

        int storyCountBefore = promptRepository.findByModeAndActiveTrue(Mode.STORY).size();

        // Run initializer again
        initializer.initializeStoryPrompts();

        // Verify OFF_THE_CUFF prompt still exists and is untouched
        Prompt retrievedOtc = promptRepository.findById(savedOtc.getId()).orElse(null);
        assertNotNull(retrievedOtc);
        assertEquals("Impromptu speech topic", retrievedOtc.getText());
        assertEquals(Mode.OFF_THE_CUFF, retrievedOtc.getMode());

        // Verify mode isolation: findByModeAndActiveTrue(STORY) only returns STORY
        List<Prompt> storyList = promptRepository.findByModeAndActiveTrue(Mode.STORY);
        assertTrue(storyList.stream().allMatch(p -> p.getMode() == Mode.STORY));

        // Verify story prompt count was unaffected
        int storyCountAfter = promptRepository.findByModeAndActiveTrue(Mode.STORY).size();
        assertEquals(storyCountBefore, storyCountAfter);
    }

    @Test
    void testStoryCategoriesAndRandomPrompt() {
        var categories = promptService.getCategories(Mode.STORY);
        assertFalse(categories.isEmpty(), "Story categories should not be empty");
        assertEquals(6, categories.size(), "Should have 6 Story categories");

        var prompt = promptService.getRandomPrompt(Mode.STORY, null, null);
        assertNotNull(prompt);
        assertEquals("STORY", prompt.getMode());

        // Test category filtering for story
        var advPrompt = promptService.getRandomPrompt(Mode.STORY, "Adventure & Survival", null);
        assertNotNull(advPrompt);
        assertEquals("Adventure & Survival", advPrompt.getCategory());
        assertEquals("STORY", advPrompt.getMode());

        // Test excludeId
        Long excludedId = advPrompt.getId();
        var nextPrompt = promptService.getRandomPrompt(Mode.STORY, "Adventure & Survival", excludedId);
        assertNotNull(nextPrompt);
        assertNotEquals(excludedId, nextPrompt.getId());
    }
}
