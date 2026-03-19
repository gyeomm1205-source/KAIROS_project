package com.ssafy.springbootbe.domain.auth.exception;

public class GoogleAuthorizationCodeMissingException extends RuntimeException {

    public GoogleAuthorizationCodeMissingException() {
        super("Google 인가 코드가 없거나 올바르지 않습니다.");
    }
}
