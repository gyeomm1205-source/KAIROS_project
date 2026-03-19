package com.ssafy.springbootbe.domain.auth.exception;

public class GithubRedirectGenerationException extends RuntimeException {

    public GithubRedirectGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
