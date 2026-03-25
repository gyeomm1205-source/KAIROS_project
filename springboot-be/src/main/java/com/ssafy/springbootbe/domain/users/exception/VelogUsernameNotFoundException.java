package com.ssafy.springbootbe.domain.users.exception;

public class VelogUsernameNotFoundException extends RuntimeException {

    public VelogUsernameNotFoundException(Long userId) {
        super("Velog username을 찾을 수 없습니다. userId=" + userId);
    }
}
