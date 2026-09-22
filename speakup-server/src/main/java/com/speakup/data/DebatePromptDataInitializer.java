package com.speakup.data;

import com.speakup.model.Category;
import com.speakup.model.Mode;
import com.speakup.model.Prompt;
import com.speakup.repository.CategoryRepository;
import com.speakup.repository.PromptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.*;

/**
 * Idempotent data initializer for Debate Mode prompts.
 * Automatically loads prompts from debate-prompts.json and inserts only prompts
 * that do not already exist in the database for Mode.DEBATE.
 * Uses database-generated IDs and never deletes existing records.
 */
@Component
public class DebatePromptDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DebatePromptDataInitializer.class);

    private final PromptRepository promptRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;

    public DebatePromptDataInitializer(
            PromptRepository promptRepository,
            CategoryRepository categoryRepository,
            ObjectMapper objectMapper) {
        this.promptRepository = promptRepository;
        this.categoryRepository = categoryRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int seeded = initializeDebatePrompts();
        if (seeded > 0) {
            log.info("DebatePromptDataInitializer: Successfully seeded {} debate prompts", seeded);
        } else {
            log.info("DebatePromptDataInitializer: No new debate prompts to seed");
        }
    }

    @Transactional
    public int initializeDebatePrompts() {
        ClassPathResource resource = new ClassPathResource("debate-prompts.json");
        if (!resource.exists()) {
            log.warn("debate-prompts.json not found on classpath, skipping initialization");
            return 0;
        }

        JsonNode root;
        try (InputStream is = resource.getInputStream()) {
            root = objectMapper.readTree(is);
        } catch (Exception e) {
            log.error("Failed to read debate-prompts.json: {}", e.getMessage(), e);
            return 0;
        }

        if (root == null || !root.isArray() || root.isEmpty()) {
            return 0;
        }

        // Fetch existing prompt texts for Mode.DEBATE in one query to avoid N+1 queries
        Set<String> existingTexts = promptRepository.findTextsByMode(Mode.DEBATE);
        if (existingTexts == null) {
            existingTexts = Collections.emptySet();
        }

        Map<String, Category> categoryCache = new HashMap<>();
        List<Prompt> newPrompts = new ArrayList<>();

        for (JsonNode item : root) {
            String text = item.path("text").asText("").trim();
            if (text.isEmpty() || existingTexts.contains(text)) {
                continue;
            }

            String categoryName = item.path("category").asText("General").trim();
            if (categoryName.isEmpty()) {
                categoryName = "General";
            }

            Category category = categoryCache.computeIfAbsent(categoryName, name ->
                    categoryRepository.findByName(name)
                            .orElseGet(() -> categoryRepository.save(new Category(name, name + " debate topics")))
            );

            Prompt prompt = new Prompt();
            prompt.setText(text);
            prompt.setCategory(category);
            prompt.setMode(Mode.DEBATE);
            prompt.setActive(true);

            newPrompts.add(prompt);
        }

        if (!newPrompts.isEmpty()) {
            promptRepository.saveAll(newPrompts);
        }

        return newPrompts.size();
    }
}
