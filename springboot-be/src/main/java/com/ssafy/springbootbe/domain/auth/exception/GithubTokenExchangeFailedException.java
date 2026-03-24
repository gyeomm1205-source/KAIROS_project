package com.ssafy.springbootbe.domain.auth.exception;

public class GithubTokenExchangeFailedException extends RuntimeException {

    public GithubTokenExchangeFailedException(String message) {
        super(message);
    }

    public GithubTokenExchangeFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
