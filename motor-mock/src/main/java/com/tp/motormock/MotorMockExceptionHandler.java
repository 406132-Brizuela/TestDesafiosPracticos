package com.tp.motormock;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class MotorMockExceptionHandler {

    @ExceptionHandler(MotorChallengeNotFoundException.class)
    ProblemDetail notFound(MotorChallengeNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Desafío no encontrado");
        return detail;
    }
}
