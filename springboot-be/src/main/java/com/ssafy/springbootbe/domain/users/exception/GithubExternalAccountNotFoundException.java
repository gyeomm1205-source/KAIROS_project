package com.ssafy.springbootbe.domain.users.exception;

public class GithubExternalAccountNotFoundException extends RuntimeException {

    public GithubExternalAccountNotFoundException(Long userId) {
        super("GitHub 연동 계정을 찾을 수 없습니다. userId=" + userId);
    }
}
