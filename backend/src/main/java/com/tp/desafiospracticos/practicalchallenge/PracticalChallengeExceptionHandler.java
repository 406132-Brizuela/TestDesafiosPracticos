package com.tp.desafiospracticos.practicalchallenge;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        PracticalChallengeController.class,
        AttemptQueryController.class,
        LocalAttemptCreationController.class
})
public class PracticalChallengeExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validationError(MethodArgumentNotValidException exception) {
        Map<String, String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(), "Los datos enviados no son válidos", fields));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadableRequest() {
        return ResponseEntity.badRequest().body(new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "El cuerpo contiene un valor inválido",
                Map.of()
        ));
    }

    @ExceptionHandler(PracticalChallengeNotFoundException.class)
    ResponseEntity<ApiError> notFound(PracticalChallengeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                HttpStatus.NOT_FOUND.value(), exception.getMessage(), Map.of()));
    }

    public record ApiError(int status, String message, Map<String, String> fields) {
    }
}
