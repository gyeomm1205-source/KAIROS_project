package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationQuizGenerationException extends RuntimeException {

    public RecommendationQuizGenerationException(String message) {
        super(message);
    }

    public RecommendationQuizGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
