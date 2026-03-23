package com.ssafy.springbootbe.common.exception;

import com.ssafy.springbootbe.domain.activities.exception.ActivityAccessDeniedException;
import com.ssafy.springbootbe.domain.activities.exception.ActivityNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeNotFoundException;
import com.ssafy.springbootbe.domain.calendar.exception.GoogleOAuthNotFoundException;
import com.ssafy.springbootbe.domain.auth.exception.AuthRedisSaveFailedException;
import com.ssafy.springbootbe.domain.auth.exception.AuthCookieProcessingException;
import com.ssafy.springbootbe.domain.auth.exception.AuthPersistenceException;
import com.ssafy.springbootbe.domain.auth.exception.AuthTokenGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.AlreadyUsedRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateGithubAccountException;
import com.ssafy.springbootbe.domain.auth.exception.DuplicateOAuthEmailException;
import com.ssafy.springbootbe.domain.auth.exception.GithubAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GithubRedirectGenerationException;
import com.ssafy.springbootbe.domain.auth.exception.GithubTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GithubUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleAuthorizationCodeMissingException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleTokenExchangeFailedException;
import com.ssafy.springbootbe.domain.auth.exception.GoogleUserInfoFetchFailedException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidAccessTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidRefreshTokenException;
import com.ssafy.springbootbe.domain.auth.exception.InvalidOnboardingTokenException;
import com.ssafy.springbootbe.domain.auth.exception.VelogCollectAsyncFailedException;
import com.ssafy.springbootbe.domain.onboarding.exception.AnalysisReportPreparationException;
import com.ssafy.springbootbe.domain.onboarding.exception.InvalidSurveyInputException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingAccessDeniedException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingMetaRetrievalException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingPersistenceException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingReferenceNotFoundException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingUserNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationCurriculumRetrievalException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationAccessDeniedException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationDetailNotFoundException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationNodeAggregationException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationPayloadParsingException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationQuizGenerationException;
import com.ssafy.springbootbe.domain.recommendations.exception.RecommendationRedisLookupException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAccessDeniedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizActivityHistoryPersistenceException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAlreadyCompletedException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizAnswerConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizCurriculumNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizGenerationException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizIncompleteException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizPayloadParsingException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizQuestionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizRedisCleanupException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizRedisException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionConflictException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionNotFoundException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSessionPersistenceException;
import com.ssafy.springbootbe.domain.quizzes.exception.QuizSourceNotFoundException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleAccessDeniedException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleNotFoundException;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
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

    @ExceptionHandler(GithubAuthorizationCodeMissingException.class)
    public ResponseEntity<Map<String, String>> handleGithubAuthorizationCodeMissingException(
            GithubAuthorizationCodeMissingException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }

    @ExceptionHandler(GoogleTokenExchangeFailedException.class)
    public ResponseEntity<Map<String, String>> handleGoogleTokenExchangeFailedException(
            GoogleTokenExchangeFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(GithubTokenExchangeFailedException.class)
    public ResponseEntity<Map<String, String>> handleGithubTokenExchangeFailedException(
            GithubTokenExchangeFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidOnboardingTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidOnboardingTokenException(
            InvalidOnboardingTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidRefreshTokenException(
            InvalidRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidAccessTokenException.class)
    public ResponseEntity<Map<String, String>> handleInvalidAccessTokenException(
            InvalidAccessTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "INVALID_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(AlreadyUsedRefreshTokenException.class)
    public ResponseEntity<Map<String, String>> handleAlreadyUsedRefreshTokenException(
            AlreadyUsedRefreshTokenException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "ALREADY_USED_TOKEN", "message", e.getMessage()));
    }

    @ExceptionHandler(GoogleUserInfoFetchFailedException.class)
    public ResponseEntity<Map<String, String>> handleGoogleUserInfoFetchFailedException(
            GoogleUserInfoFetchFailedException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(GithubUserInfoFetchFailedException.class)
    public ResponseEntity<Map<String, String>> handleGithubUserInfoFetchFailedException(
            GithubUserInfoFetchFailedException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateOAuthEmailException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateOAuthEmailException(DuplicateOAuthEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "DUPLICATE_USER", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateGithubAccountException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateGithubAccountException(
            DuplicateGithubAccountException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "DUPLICATE_USER", "message", e.getMessage()));
    }

    @ExceptionHandler({
            AuthTokenGenerationException.class,
            AuthRedisSaveFailedException.class,
            AuthPersistenceException.class,
            AuthCookieProcessingException.class
    })
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

    @ExceptionHandler(VelogCollectAsyncFailedException.class)
    public ResponseEntity<Map<String, String>> handleVelogCollectAsyncFailedException(
            VelogCollectAsyncFailedException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "BAD_GATEWAY", "message", e.getMessage()));
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

    @ExceptionHandler(GoogleOAuthNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleGoogleOAuthNotFoundException(GoogleOAuthNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(CurriculumNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCurriculumNotFoundException(CurriculumNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(CurriculumAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleCurriculumAccessDeniedException(CurriculumAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(CurriculumNodeNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleCurriculumNodeNotFoundException(CurriculumNodeNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(CurriculumNodeAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleCurriculumNodeAccessDeniedException(CurriculumNodeAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(ActivityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleActivityNotFoundException(ActivityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(ActivityAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleActivityAccessDeniedException(ActivityAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(OnboardingUserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingUserNotFoundException(OnboardingUserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(OnboardingReferenceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingReferenceNotFoundException(
            OnboardingReferenceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(OnboardingAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleOnboardingAccessDeniedException(OnboardingAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidSurveyInputException.class)
    public ResponseEntity<Map<String, String>> handleInvalidSurveyInputException(InvalidSurveyInputException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }

    @ExceptionHandler({
            OnboardingMetaRetrievalException.class,
            OnboardingPersistenceException.class,
            AnalysisReportPreparationException.class
    })
    public ResponseEntity<Map<String, String>> handleOnboardingInternalException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler({
            RecommendationCurriculumRetrievalException.class,
            RecommendationNodeAggregationException.class,
            RecommendationRedisLookupException.class,
            RecommendationPayloadParsingException.class
    })
    public ResponseEntity<Map<String, String>> handleRecommendationInternalException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(RecommendationCurriculumNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRecommendationCurriculumNotFoundException(
            RecommendationCurriculumNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(RecommendationDetailNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleRecommendationDetailNotFoundException(
            RecommendationDetailNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(RecommendationAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleRecommendationAccessDeniedException(
            RecommendationAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(RecommendationQuizGenerationException.class)
    public ResponseEntity<Map<String, String>> handleRecommendationQuizGenerationException(
            RecommendationQuizGenerationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "BAD_GATEWAY", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizCurriculumNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleQuizCurriculumNotFoundException(
            QuizCurriculumNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler({
            QuizQuestionNotFoundException.class,
            QuizSessionNotFoundException.class,
            QuizSourceNotFoundException.class
    })
    public ResponseEntity<Map<String, String>> handleQuizNotFoundExceptions(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleQuizAccessDeniedException(QuizAccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "ACCESS_DENIED", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizSessionConflictException.class)
    public ResponseEntity<Map<String, String>> handleQuizSessionConflictException(QuizSessionConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CONFLICT", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizAnswerConflictException.class)
    public ResponseEntity<Map<String, String>> handleQuizAnswerConflictException(QuizAnswerConflictException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CONFLICT", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizAlreadyCompletedException.class)
    public ResponseEntity<Map<String, String>> handleQuizAlreadyCompletedException(QuizAlreadyCompletedException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("error", "CONFLICT", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizIncompleteException.class)
    public ResponseEntity<Map<String, String>> handleQuizIncompleteException(QuizIncompleteException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }

    @ExceptionHandler(QuizGenerationException.class)
    public ResponseEntity<Map<String, String>> handleQuizGenerationException(QuizGenerationException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "BAD_GATEWAY", "message", e.getMessage()));
    }

    @ExceptionHandler({
            QuizRedisException.class,
            QuizRedisCleanupException.class,
            QuizPayloadParsingException.class,
            QuizSessionPersistenceException.class,
            QuizActivityHistoryPersistenceException.class
    })
    public ResponseEntity<Map<String, String>> handleQuizInternalException(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "SERVER_ERROR", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", message != null ? message : "입력값이 올바르지 않습니다."));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", "요청 본문이 올바르지 않습니다."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "INVALID_INPUT", "message", e.getMessage()));
    }
}
