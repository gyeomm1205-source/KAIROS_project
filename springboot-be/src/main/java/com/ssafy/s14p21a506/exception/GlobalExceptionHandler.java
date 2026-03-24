package com.ssafy.s14p21a506.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_LOG_FORMAT = "[{}] {} {} - {}";

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException e, HttpServletRequest request) {
        HttpStatus httpStatus = e.getHttpStatus();

        if (httpStatus.is5xxServerError()) {
            logError(e.getErrorCode(), request, e.getMessage(), e);
        } else {
            logWarn(e.getErrorCode(), request, e.getMessage());
        }

        ErrorResponse response = new ErrorResponse(e.getErrorCode(), request.getRequestURI());
        return ResponseEntity.status(httpStatus).body(response);
    }

    @ExceptionHandler({
            BindException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidInput(Exception e, HttpServletRequest request) {
        String logDetail = summarizeInvalidInput(e);
        logInfo(request, logDetail);

        ErrorResponse response = new ErrorResponse(ErrorCode.INVALID_INPUT_VALUE, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e, HttpServletRequest request) {
        logError(ErrorCode.INTERNAL_SERVER_ERROR, request, e.getMessage(), e);

        ErrorResponse response = new ErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    private static String summarizeInvalidInput(Exception e) {
        if (e instanceof BindException bindException) {
            return bindException.getBindingResult().getFieldErrors().stream()
                    .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("Invalid request value.");
        }

        if (e instanceof ConstraintViolationException violationException) {
            return violationException.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .reduce((left, right) -> left + ", " + right)
                    .orElse("Invalid request value.");
        }

        if (e instanceof HttpMessageNotReadableException) {
            return "Please check request body format.";
        }

        String message = e.getMessage();
        return (message == null || message.isBlank()) ? "Invalid request value." : message;
    }

    private static void logInfo(HttpServletRequest request, String detail) {
        log.info(REQUEST_LOG_FORMAT, ErrorCode.INVALID_INPUT_VALUE.getCode(), request.getMethod(), request.getRequestURI(), detail);
    }

    private static void logWarn(ErrorCode errorCode, HttpServletRequest request, String detail) {
        log.warn(REQUEST_LOG_FORMAT, errorCode.getCode(), request.getMethod(), request.getRequestURI(), detail);
    }

    private static void logError(ErrorCode errorCode, HttpServletRequest request, String detail, Throwable throwable) {
        log.error(REQUEST_LOG_FORMAT, errorCode.getCode(), request.getMethod(), request.getRequestURI(), detail, throwable);
    }
}
