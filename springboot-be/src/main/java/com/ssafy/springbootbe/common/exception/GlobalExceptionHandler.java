package com.ssafy.springbootbe.common.exception;

import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.AuthTokenGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GithubRedirectGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleAccessDeniedException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleNotFoundException;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GoogleAuthorizationCodeMissingException.class)
    public ResponseEntity<Map<String, String>> handleGoogleAuthorizationCodeMissingException(
            GoogleAuthorizationCodeMissingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }

    @ExceptionHandler(GoogleTokenExchangeFailedException.class)
    public ResponseEntity<Map<String, String>> handleGoogleTokenExchangeFailedException(
            GoogleTokenExchangeFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidOnboardingTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOnboardingTokenException(
            InvalidOnboardingTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(GoogleUserInfoFetchFailedException.class)
    public ResponseEntity<Map<String, String>> handleGoogleUserInfoFetchFailedException(
            GoogleUserInfoFetchFailedException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateOAuthEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateOAuthEmailException(DuplicateOAuthEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "DUPLICATE_USER", "message", e.getMessage()));
    }

    @ExceptionHandler({AuthTokenGenerationException.class, AuthRedisSaveFailedException.class})
    public ResponseEntity<Map<String, String>> handleAuthInternalException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(GithubRedirectGenerationException.class)
    public ResponseEntity<Map<String, String>> handleGithubRedirectGenerationException(
            GithubRedirectGenerationException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(ScheduleNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleScheduleNotFoundException(ScheduleNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(ScheduleAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleScheduleAccessDeniedException(ScheduleAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFoundException(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", message != null ? message : "입력값이 올바르지 않습니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }
}
