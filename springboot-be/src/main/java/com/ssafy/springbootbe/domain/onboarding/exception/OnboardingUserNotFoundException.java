package com.ssafy.springbootbe.domain.onboarding.exception;

public class OnboardingUserNotFoundException extends RuntimeException {

    public OnboardingUserNotFoundException(Long userId) {
        super("온보딩 대상 사용자를 찾을 수 없습니다. userId=" + userId);
    }
}
