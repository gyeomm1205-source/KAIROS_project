package com.ssafy.springbootbe.domain.auth.exception;

public class AuthRedisSaveFailedException extends RuntimeException {

    public AuthRedisSaveFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
