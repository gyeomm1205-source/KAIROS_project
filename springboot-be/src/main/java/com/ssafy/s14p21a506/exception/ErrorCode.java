package com.ssafy.s14p21a506.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    MOCK_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "MOCK-POST-001", "MockPost not found."),
    FASTAPI_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "TEST-PING-001", "FastAPI is unavailable."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "AUTH-001", "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "AUTH-002", "You do not have permission to access this resource."),

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-001", "Internal server error."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-002", "Invalid request value.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
