package com.tp.desafiospracticos.tutor;

public interface TutorSessionClient {

    TutorSession createSession(String attemptId, String practicalChallengeId);

    TutorMessage sendMessage(String sessionId, TutorMessageCommand command);
}
