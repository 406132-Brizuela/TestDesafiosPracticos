package com.tp.desafiospracticos.tutor;

public record TutorMessageCommand(
        String attemptId,
        String content,
        TutorContext context
) {
    public record TutorContext(
            String practicalChallengeId,
            String statement,
            String starterCode,
            String currentCode
    ) {
    }
}
