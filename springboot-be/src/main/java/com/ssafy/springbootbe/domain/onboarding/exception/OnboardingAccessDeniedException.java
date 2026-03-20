package com.ssafy.springbootbe.domain.onboarding.exception;

import com.ssafy.springbootbe.persistence.user.type.UserStatus;

public class OnboardingAccessDeniedException extends RuntimeException {

    public OnboardingAccessDeniedException(Long userId, UserStatus userStatus) {
        super("온보딩 설문은 GUEST 상태에서만 가능합니다. userId=" + userId + ", status=" + userStatus);
    }
}
