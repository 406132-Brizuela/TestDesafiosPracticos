package com.tp.desafiospracticos.practicalchallenge;

import com.tp.desafiospracticos.motor.MotorChallengeNotFoundException;
import com.tp.desafiospracticos.motor.MotorCatalogController;
import com.tp.desafiospracticos.tutor.TutorServiceUnavailableException;
import com.tp.desafiospracticos.tutor.TutorAccessDeniedException;
import com.tp.desafiospracticos.tutor.TutorInteractionNotAllowedException;
import com.tp.desafiospracticos.tutor.TutorSessionController;
import org.springframework.dao.DataIntegrityViolationException;
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
        LocalAttemptCreationController.class,
        LocalAttemptDraftController.class,
        MotorCatalogController.class,
        TutorSessionController.class
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

    @ExceptionHandler(AttemptNotFoundException.class)
    ResponseEntity<ApiError> notFound(AttemptNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(
                HttpStatus.NOT_FOUND.value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MotorChallengeNotFoundException.class)
    ResponseEntity<ApiError> motorChallengeNotFound(MotorChallengeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ApiError(
                HttpStatus.UNPROCESSABLE_ENTITY.value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TutorServiceUnavailableException.class)
    ResponseEntity<ApiError> tutorUnavailable(TutorServiceUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError(
                HttpStatus.BAD_GATEWAY.value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TutorAccessDeniedException.class)
    ResponseEntity<ApiError> tutorAccessDenied(TutorAccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(
                HttpStatus.FORBIDDEN.value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(TutorInteractionNotAllowedException.class)
    ResponseEntity<ApiError> tutorInteractionNotAllowed(
            TutorInteractionNotAllowedException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                HttpStatus.CONFLICT.value(), exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(
                HttpStatus.CONFLICT.value(),
                "El desafío o intento ya existe",
                Map.of()
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> internalError(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(), exception.getMessage(), Map.of()));
    }

    public record ApiError(int status, String message, Map<String, String> fields) {
    }
}
