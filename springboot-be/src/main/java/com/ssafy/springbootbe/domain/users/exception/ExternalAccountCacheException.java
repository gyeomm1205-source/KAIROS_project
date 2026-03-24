package com.ssafy.springbootbe.domain.users.exception;

public class ExternalAccountCacheException extends RuntimeException {

    public ExternalAccountCacheException(String message, Throwable cause) {
        super(message, cause);
    }
}
