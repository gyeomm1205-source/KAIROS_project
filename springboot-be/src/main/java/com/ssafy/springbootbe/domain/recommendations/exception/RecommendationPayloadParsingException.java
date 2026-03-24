package com.ssafy.springbootbe.domain.recommendations.exception;

public class RecommendationPayloadParsingException extends RuntimeException {

    public RecommendationPayloadParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
