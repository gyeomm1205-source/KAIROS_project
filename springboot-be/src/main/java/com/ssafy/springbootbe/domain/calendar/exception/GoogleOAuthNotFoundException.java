package com.ssafy.springbootbe.domain.calendar.exception;

public class GoogleOAuthNotFoundException extends RuntimeException {

    public GoogleOAuthNotFoundException(Long userId) {
        super("Google OAuth 계정을 찾을 수 없습니다. userId=" + userId);
    }
}