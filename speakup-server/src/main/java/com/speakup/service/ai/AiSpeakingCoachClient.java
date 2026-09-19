package com.speakup.service.ai;

public interface AiSpeakingCoachClient {

    /**
     * Analyze a completed speaking session transcript and produce structured coaching feedback.
     *
     * @param mode                  Speaking mode (e.g. OFF_THE_CUFF)
     * @param topic                 The prompt/topic text
     * @param category              Category of the topic (if available)
     * @param targetDurationSeconds Planned timer duration
     * @param actualDurationSeconds Actual speaking duration
     * @param transcript            Spoken transcript text
     * @return Structured feedback response containing scores, summary, strengths, and improvements
     */
    AiFeedbackResponse analyzeSpeaking(
            String mode,
            String topic,
            String category,
            int targetDurationSeconds,
            int actualDurationSeconds,
            String transcript
    );

    /**
     * Analyze a completed speaking session transcript with optional preparation notes.
     */
    default AiFeedbackResponse analyzeSpeaking(
            String mode,
            String topic,
            String category,
            int targetDurationSeconds,
            int actualDurationSeconds,
            String transcript,
            String preparationNotes
    ) {
        return analyzeSpeaking(mode, topic, category, targetDurationSeconds, actualDurationSeconds, transcript);
    }
}
