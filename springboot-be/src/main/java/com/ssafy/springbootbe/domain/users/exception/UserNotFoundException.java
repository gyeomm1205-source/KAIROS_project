package com.ssafy.springbootbe.domain.users.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long userId) {
        super("사용자를 찾을 수 없습니다. userId=" + userId);
    }
}
