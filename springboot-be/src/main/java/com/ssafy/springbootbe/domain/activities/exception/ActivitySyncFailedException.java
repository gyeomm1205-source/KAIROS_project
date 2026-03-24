package com.ssafy.springbootbe.domain.activities.exception;

public class ActivitySyncFailedException extends RuntimeException {

    public ActivitySyncFailedException(String provider, Throwable cause) {
        super(provider + " 동기화 중 외부 API 호출에 실패했습니다.", cause);
    }
}
