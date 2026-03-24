package com.ssafy.springbootbe.domain.users.exception;

public class ExternalAccountFetchFailedException extends RuntimeException {

    public ExternalAccountFetchFailedException(String message) {
        super(message);
    }

    public ExternalAccountFetchFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
