package com.speakup.mapper;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.speakup.dto.FeedbackDto;
import com.speakup.model.Feedback;

import java.util.Collections;
import java.util.List;

public class FeedbackMapper {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    private FeedbackMapper() {
        // Utility class
    }

    public static FeedbackDto toDto(Feedback feedback) {
        if (feedback == null) {
            return null;
        }

        return new FeedbackDto(
                feedback.getId(),
                feedback.getSession() != null ? feedback.getSession().getId() : null,
                feedback.getOverallScore(),
                feedback.getClarityScore(),
                feedback.getRelevanceScore(),
                feedback.getStructureScore(),
                feedback.getSummary(),
                deserializeList(feedback.getStrengths()),
                deserializeList(feedback.getImprovements()),
                feedback.getCreatedAt()
        );
    }

    public static String serializeList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(list);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<String> deserializeList(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(json, STRING_LIST_TYPE);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
