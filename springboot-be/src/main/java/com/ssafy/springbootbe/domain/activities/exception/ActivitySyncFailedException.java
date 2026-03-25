package com.ssafy.springbootbe.domain.activities.exception;

public class ActivitySyncFailedException extends RuntimeException {

    public ActivitySyncFailedException(String provider, Throwable cause) {
        super(provider + " 동기화 중 외부 연동 처리에 실패했습니다.", cause);
    }

    public ActivitySyncFailedException(String message, Throwable cause, boolean customMessage) {
        super(message, cause);
    }
}
