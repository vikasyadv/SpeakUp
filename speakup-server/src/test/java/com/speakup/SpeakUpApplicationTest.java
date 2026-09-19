package com.speakup;

import com.speakup.data.ResearchPromptDataInitializer;
import com.speakup.model.Mode;
import com.speakup.repository.PromptRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
class SpeakUpApplicationTest {

    @Autowired
    private ResearchPromptDataInitializer researchPromptDataInitializer;

    @Autowired
    private PromptRepository promptRepository;

    @Test
    void contextLoadsAndInitializerExecutes() {
        assertNotNull(researchPromptDataInitializer, "Initializer bean must be registered in context");
        assertFalse(promptRepository.findByModeAndActiveTrue(Mode.RESEARCH).isEmpty(),
                "Research prompts must be automatically populated on application startup");
    }
}
