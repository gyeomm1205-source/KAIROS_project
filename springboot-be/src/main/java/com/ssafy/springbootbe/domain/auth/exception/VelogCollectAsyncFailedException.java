package com.ssafy.springbootbe.domain.auth.exception;

public class VelogCollectAsyncFailedException extends RuntimeException {

    public VelogCollectAsyncFailedException(String message) {
        super(message);
    }

    public VelogCollectAsyncFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
