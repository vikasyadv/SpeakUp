package com.speakup.data;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ResearchPromptDataValidationTest {

    private static final List<String> EXPECTED_CATEGORIES = List.of(
            "Technology",
            "Science",
            "Environment",
            "Education",
            "Society",
            "Business & Economy",
            "Politics & Civics",
            "Psychology",
            "Health & Lifestyle",
            "Ethics",
            "Culture",
            "Future"
    );

    private static record PromptEntry(String category, String text) {}

    private static List<PromptEntry> prompts;

    @BeforeAll
    static void loadPrompts() throws Exception {
        ClassPathResource resource = new ClassPathResource("research-prompts.json");
        assertTrue(resource.exists(), "research-prompts.json must exist on classpath");

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream is = resource.getInputStream()) {
            JsonNode root = mapper.readTree(is);
            assertTrue(root.isArray(), "Root JSON element must be an array");

            prompts = new ArrayList<>();
            for (JsonNode node : root) {
                assertTrue(node.has("category"), "Prompt entry must have category field");
                assertTrue(node.has("text"), "Prompt entry must have text field");

                String category = node.get("category").asText();
                String text = node.get("text").asText();
                prompts.add(new PromptEntry(category, text));
            }
        }
    }

    @Test
    void testTotalPromptCountIsExactly1200() {
        assertEquals(1200, prompts.size(), "Dataset must contain exactly 1,200 prompts");
    }

    @Test
    void testAllCategoriesPresentAndBalancedAt100Each() {
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (PromptEntry entry : prompts) {
            assertNotNull(entry.category(), "Category must not be null");
            assertFalse(entry.category().isBlank(), "Category must not be blank");
            categoryCounts.put(entry.category(), categoryCounts.getOrDefault(entry.category(), 0) + 1);
        }

        assertEquals(12, categoryCounts.size(), "Exactly 12 distinct categories must be represented");

        for (String expectedCat : EXPECTED_CATEGORIES) {
            assertTrue(categoryCounts.containsKey(expectedCat), "Expected category missing: " + expectedCat);
            assertEquals(100, categoryCounts.get(expectedCat),
                    "Category '" + expectedCat + "' must have exactly 100 prompts");
        }
    }

    @Test
    void testWordCountsAreValid() {
        for (int i = 0; i < prompts.size(); i++) {
            PromptEntry entry = prompts.get(i);
            String text = entry.text();
            assertNotNull(text, "Prompt text must not be null at index " + i);
            assertFalse(text.isBlank(), "Prompt text must not be blank at index " + i);

            String[] words = text.trim().split("\\s+");
            int wordCount = words.length;

            assertTrue(wordCount >= 5 && wordCount <= 25,
                    String.format("Prompt at index %d ('%s'...) has %d words, expected between 5 and 25: %s",
                            i, text.substring(0, Math.min(text.length(), 30)), wordCount, text));
        }
    }

    @Test
    void testNoExactDuplicates() {
        Set<String> uniqueTexts = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (PromptEntry entry : prompts) {
            if (!uniqueTexts.add(entry.text())) {
                duplicates.add(entry.text());
            }
        }

        assertTrue(duplicates.isEmpty(), "Found exact duplicate prompts: " + duplicates);
    }

    @Test
    void testNoCaseInsensitiveDuplicates() {
        Set<String> lowerTexts = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (PromptEntry entry : prompts) {
            String lower = entry.text().toLowerCase(Locale.ROOT).trim();
            if (!lowerTexts.add(lower)) {
                duplicates.add(entry.text());
            }
        }

        assertTrue(duplicates.isEmpty(), "Found case-insensitive duplicate prompts: " + duplicates);
    }

    @Test
    void testNoPunctuationNormalizedDuplicates() {
        Set<String> normalizedTexts = new HashSet<>();
        List<String> duplicates = new ArrayList<>();

        for (PromptEntry entry : prompts) {
            String normalized = entry.text().replaceAll("[^a-zA-Z0-9\\s]", "").toLowerCase(Locale.ROOT).trim();
            if (!normalizedTexts.add(normalized)) {
                duplicates.add(entry.text() + " (normalized: " + normalized + ")");
            }
        }

        assertTrue(duplicates.isEmpty(), "Found punctuation-normalized duplicate prompts: " + duplicates);
    }
}
