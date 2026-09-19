package com.tp.llmmock;

import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/llm/tutor/sessions")
public class TutorSessionController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TutorSessionController.class);

    private final TutorSessionCatalog catalog;

    public TutorSessionController(TutorSessionCatalog catalog) {
        this.catalog = catalog;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TutorSession create(@Valid @RequestBody TutorSessionCreateRequest request) {
        TutorSession session = catalog.create(request);
        LOGGER.info("Tutor session ready: sessionId={}, attemptId={}, practicalChallengeId={}",
                session.sessionId(), session.attemptId(), session.practicalChallengeId());
        return session;
    }

    @PostMapping("/{sessionId}/messages")
    public TutorMessage sendMessage(@PathVariable String sessionId,
                                    @Valid @RequestBody TutorMessageCreateRequest request) {
        LOGGER.info("Tutor message received: sessionId={}, attemptId={}",
                sessionId, request.attemptId());
        TutorMessage response = catalog.reply(sessionId, request);
        LOGGER.info("Mock tutor response sent: sessionId={}, messageId={}",
                sessionId, response.messageId());
        return response;
    }
}
