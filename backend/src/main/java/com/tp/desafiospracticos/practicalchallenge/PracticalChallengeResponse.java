package com.tp.desafiospracticos.practicalchallenge;

import java.util.List;

public record PracticalChallengeResponse(
        String id,
        String title,
        String statement,
        Difficulty difficulty,
        ChallengeType type,
        ProgrammingLanguage language,
        String starterCode,
        List<TestCaseResponse> testCases
) {
    public record TestCaseResponse(
            Long id,
            String name,
            String input,
            String expectedOutput,
            TestVisibility visibility
    ) {
    }
}
