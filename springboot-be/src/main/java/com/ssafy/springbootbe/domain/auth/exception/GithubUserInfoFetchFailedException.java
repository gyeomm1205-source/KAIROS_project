package com.ssafy.springbootbe.domain.auth.exception;

public class GithubUserInfoFetchFailedException extends RuntimeException {

    public GithubUserInfoFetchFailedException(String message) {
        super(message);
    }

    public GithubUserInfoFetchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
