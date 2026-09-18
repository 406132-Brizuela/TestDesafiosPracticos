package com.tp.desafiospracticos.challenge;

import com.tp.desafiospracticos.web.ChallengeController;
import com.tp.desafiospracticos.web.EvaluationController;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduce {@link ChallengeNotFoundException} (challengeId inexistente, tanto para
 * el GET del desafío como para /engine/evaluate) a 404 en vez del whitelabel 500
 * por defecto. Mismo patrón que {@code PracticalChallengeExceptionHandler} y
 * {@code MotorMockExceptionHandler}.
 */
@RestControllerAdvice(assignableTypes = {ChallengeController.class, EvaluationController.class})
public class ChallengeExceptionHandler {

    @ExceptionHandler(ChallengeNotFoundException.class)
    ProblemDetail notFound(ChallengeNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Desafío no encontrado");
        return detail;
    }
}
