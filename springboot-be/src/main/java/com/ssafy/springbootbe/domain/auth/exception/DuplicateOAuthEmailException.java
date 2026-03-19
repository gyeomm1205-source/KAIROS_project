package com.ssafy.springbootbe.domain.auth.exception;

public class DuplicateOAuthEmailException extends RuntimeException {

    public DuplicateOAuthEmailException(String email) {
        super("이미 동일한 이메일로 가입된 계정이 존재합니다. email=" + email);
    }
}
