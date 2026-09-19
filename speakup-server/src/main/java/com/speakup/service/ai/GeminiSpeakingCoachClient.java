package com.speakup.service.ai;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.speakup.exception.AiServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiSpeakingCoachClient implements AiSpeakingCoachClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiSpeakingCoachClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String apiUrl;
    private final String model;

    public GeminiSpeakingCoachClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}") String apiUrl,
            @Value("${gemini.model:gemini-3.6-flash}") String model,
            ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.apiUrl = apiUrl;
        this.model = model;
    }

    @Override
    public AiFeedbackResponse analyzeSpeaking(
            String mode,
            String topic,
            String category,
            int targetDurationSeconds,
            int actualDurationSeconds,
            String transcript) {
        return analyzeSpeaking(mode, topic, category, targetDurationSeconds, actualDurationSeconds, transcript, null);
    }

    @Override
    public AiFeedbackResponse analyzeSpeaking(
            String mode,
            String topic,
            String category,
            int targetDurationSeconds,
            int actualDurationSeconds,
            String transcript,
            String preparationNotes) {

        if (apiKey.isEmpty()) {
            throw new AiServiceException("Gemini API key is not configured on the server");
        }

        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(mode, topic, category, targetDurationSeconds, actualDurationSeconds, transcript, preparationNotes);

        log.info("Requesting Gemini AI analysis for topic: '{}', mode: '{}'", topic, mode);

        try {
            Map<String, Object> requestBody = buildGeminiRequestBody(systemPrompt, userPrompt);

            String endpointUrl = String.format("%s/%s:generateContent", apiUrl, model);

            String responseBody = restClient.post()
                    .uri(endpointUrl)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseGeminiResponse(responseBody);
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to communicate with Gemini AI: {}", e.getMessage());
            throw new AiServiceException("AI feedback generation failed. Please try again later.", e);
        }
    }

    private Map<String, Object> buildGeminiRequestBody(String systemPrompt, String userPrompt) {
        Map<String, Object> body = new HashMap<>();

        // System instruction
        Map<String, Object> systemInstruction = Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
        );
        body.put("systemInstruction", systemInstruction);

        // User content
        Map<String, Object> userContent = Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        );
        body.put("contents", List.of(userContent));

        // Generation config demanding structured JSON
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.3);
        body.put("generationConfig", generationConfig);

        return body;
    }

    private String buildSystemPrompt() {
        return """
                You are SpeakUp's AI Speaking Coach.
                Your role is to give supportive, concise, and highly actionable practice feedback based on a speech transcript.

                CRITICAL GUIDELINES:
                1. You only have access to the transcript text, speaking duration, prompt topic, and optional preparation notes.
                2. Do NOT evaluate or claim to measure vocal tone, volume, pronunciation, body language, facial expression, eye contact, microphone quality, emotional state, or precise speaking speed.
                3. Focus exclusively on:
                   - Clarity (conciseness, word choice, clarity of thought)
                   - Relevance (how well the speaker addressed the specific topic or research question)
                   - Structure (introduction/hook, logical progression, conclusion)
                   - Overall effectiveness
                   - For Research mode sessions: evaluate how clearly the speaker answered the research question, whether the explanation was well-organized and supported by evidence or examples reflected in the transcript, and how effectively any preparation notes were developed into the spoken response.
                4. Give constructive, encouraging, and specific feedback. Avoid generic filler.
                5. Scores MUST be integers between 1 and 10 (1 = poor, 10 = exceptional).

                You must return your output strictly in the following JSON format:
                {
                  "overallScore": <integer 1-10>,
                  "clarityScore": <integer 1-10>,
                  "relevanceScore": <integer 1-10>,
                  "structureScore": <integer 1-10>,
                  "summary": "<concise 2-3 sentence coaching assessment>",
                  "strengths": [
                    "<specific strength observed in the speech>",
                    "<second strength>"
                  ],
                  "improvements": [
                    "<specific, actionable improvement tip>",
                    "<second actionable tip>"
                  ]
                }
                """;
    }

    private String buildUserPrompt(
            String mode,
            String topic,
            String category,
            int targetDurationSeconds,
            int actualDurationSeconds,
            String transcript,
            String preparationNotes) {

        StringBuilder sb = new StringBuilder();
        sb.append("Please evaluate the following speaking practice session:\n\n");
        sb.append(String.format("- Mode: %s\n", mode != null ? mode : "OFF_THE_CUFF"));
        sb.append(String.format("- Topic: %s\n", topic != null ? topic : "Unknown"));
        sb.append(String.format("- Category: %s\n", category != null ? category : "General"));
        sb.append(String.format("- Target Duration: %d seconds\n", targetDurationSeconds));
        sb.append(String.format("- Actual Duration: %d seconds\n", actualDurationSeconds));
        if (preparationNotes != null && !preparationNotes.isBlank()) {
            sb.append(String.format("- Preparation Notes:\n\"\"\"\n%s\n\"\"\"\n", preparationNotes.trim()));
        }
        sb.append(String.format("- Transcript:\n\"\"\"\n%s\n\"\"\"\n", transcript != null ? transcript.trim() : ""));
        return sb.toString();
    }

    private AiFeedbackResponse parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new AiServiceException("Invalid response received from Gemini AI: no candidates found");
            }

            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
            if (textNode.isMissingNode() || textNode.asText().isBlank()) {
                throw new AiServiceException("Empty response received from Gemini AI");
            }

            String contentText = textNode.asText().trim();
            // Strip markdown block if present
            if (contentText.startsWith("```json")) {
                contentText = contentText.substring(7);
            }
            if (contentText.startsWith("```")) {
                contentText = contentText.substring(3);
            }
            if (contentText.endsWith("```")) {
                contentText = contentText.substring(0, contentText.length() - 3);
            }
            contentText = contentText.trim();

            JsonNode parsed = objectMapper.readTree(contentText);

            int overallScore = clampScore(parsed.path("overallScore").asInt(7));
            int clarityScore = clampScore(parsed.path("clarityScore").asInt(7));
            int relevanceScore = clampScore(parsed.path("relevanceScore").asInt(7));
            int structureScore = clampScore(parsed.path("structureScore").asInt(7));

            String summary = parsed.path("summary").asText("Good practice session.");

            List<String> strengths = new ArrayList<>();
            JsonNode strengthsNode = parsed.path("strengths");
            if (strengthsNode.isArray()) {
                for (JsonNode item : strengthsNode) {
                    if (!item.asText().isBlank()) {
                        strengths.add(item.asText().trim());
                    }
                }
            }
            if (strengths.isEmpty()) {
                strengths.add("Addressed the prompt directly.");
            }

            List<String> improvements = new ArrayList<>();
            JsonNode improvementsNode = parsed.path("improvements");
            if (improvementsNode.isArray()) {
                for (JsonNode item : improvementsNode) {
                    if (!item.asText().isBlank()) {
                        improvements.add(item.asText().trim());
                    }
                }
            }
            if (improvements.isEmpty()) {
                improvements.add("Try expanding on your points with a concrete example.");
            }

            return new AiFeedbackResponse(
                    overallScore,
                    clarityScore,
                    relevanceScore,
                    structureScore,
                    summary,
                    strengths,
                    improvements
            );
        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse Gemini response JSON: {}", e.getMessage());
            throw new AiServiceException("Failed to process AI feedback response", e);
        }
    }

    private int clampScore(int score) {
        if (score < 1) return 1;
        if (score > 10) return 10;
        return score;
    }
}
