package com.tp.desafiospracticos.tutor;

import java.time.Instant;

public record TutorMessage(
        String messageId,
        String sessionId,
        String role,
        String content,
        Instant createdAt
) {
}
