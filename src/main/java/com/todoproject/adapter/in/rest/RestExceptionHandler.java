package com.todoproject.adapter.in.rest;

import com.todoproject.adapter.in.rest.dto.Problem;
import com.todoproject.domain.error.BoardFullException;
import com.todoproject.domain.error.CardNotFoundException;
import com.todoproject.domain.error.InvalidCardContentException;
import com.todoproject.domain.error.RateLimitExceededException;
import com.todoproject.domain.error.StaleCardException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps every outcome in contracts/rest-api.yaml to {@code application/problem+json} (RFC 9457).
 * No branch here ever includes a stack trace, SQL, or an internal identifier (FR-024, security
 * CHK027, CHK028) -- each one is a fixed, pre-written sentence plus a stable code.
 */
@RestControllerAdvice(basePackages = "com.todoproject.adapter.in.rest")
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Problem> beanValidationFailed(MethodArgumentNotValidException exception) {
        List<Problem.FieldViolation> violations = exception.getBindingResult().getFieldErrors().stream()
                .map(RestExceptionHandler::toViolation)
                .toList();
        return problem(
                HttpStatus.BAD_REQUEST,
                Problem.validationFailed("The card was not saved because some fields are invalid.", violations));
    }

    @ExceptionHandler(InvalidCardContentException.class)
    ResponseEntity<Problem> domainValidationFailed(InvalidCardContentException exception) {
        Problem.FieldViolation violation = new Problem.FieldViolation(
                exception.field(), exception.code().name(), humanMessage(exception));
        return problem(
                HttpStatus.BAD_REQUEST,
                Problem.validationFailed(
                        "The card was not saved because some fields are invalid.", List.of(violation)));
    }

    @ExceptionHandler(CardNotFoundException.class)
    ResponseEntity<Problem> notFound() {
        return problem(
                HttpStatus.NOT_FOUND,
                Problem.of("card-not-found", "Card not found", 404, "This card no longer exists."));
    }

    @ExceptionHandler(StaleCardException.class)
    ResponseEntity<Problem> stale() {
        return problem(
                HttpStatus.PRECONDITION_FAILED,
                Problem.of(
                        "stale-revision",
                        "Stale revision",
                        412,
                        "This card changed since you last read it. Read it again and retry."));
    }

    @ExceptionHandler(IfMatchRequiredException.class)
    ResponseEntity<Problem> ifMatchRequired() {
        return problem(
                HttpStatus.PRECONDITION_REQUIRED,
                Problem.of(
                        "if-match-required",
                        "If-Match required",
                        428,
                        "Supply the revision you last read in an If-Match header."));
    }

    @ExceptionHandler(BoardFullException.class)
    ResponseEntity<Problem> boardFull() {
        return problem(
                HttpStatus.CONFLICT,
                Problem.of("board-full", "Board full", 409, "The board is full. Delete a card to add another."));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    ResponseEntity<Problem> rateLimited() {
        return problem(
                HttpStatus.TOO_MANY_REQUESTS,
                Problem.of(
                        "rate-limited", "Too many requests", 429, "Too many changes at once. Try again in a moment."));
    }

    private static ResponseEntity<Problem> problem(HttpStatus status, Problem body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }

    private static Problem.FieldViolation toViolation(FieldError error) {
        return new Problem.FieldViolation(error.getField(), "INVALID", error.getDefaultMessage());
    }

    private static String humanMessage(InvalidCardContentException exception) {
        return switch (exception.code()) {
            case REQUIRED -> "Informe um valor para " + exception.field() + ".";
            case TOO_LONG -> "O campo " + exception.field() + " excede o tamanho máximo.";
            case FORBIDDEN_CHARACTER -> "O campo " + exception.field() + " contém um caractere não aceito.";
            case INVALID -> "Valor inválido para " + exception.field() + ".";
        };
    }
}
