package com.ssafy.springbootbe.domain.auth.exception;

public class GithubAuthorizationCodeMissingException extends RuntimeException {

    public GithubAuthorizationCodeMissingException() {
        super("GitHub authorization code가 없습니다.");
    }
}
