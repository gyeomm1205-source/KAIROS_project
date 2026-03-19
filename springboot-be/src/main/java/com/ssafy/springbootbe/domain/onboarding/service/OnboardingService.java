package com.ssafy.springbootbe.domain.onboarding.service;

import com.ssafy.springbootbe.domain.onboarding.dto.request.OnboardingSurveyRequest;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingMetaResponse;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingSurveyResponse;

public interface OnboardingService {

    OnboardingMetaResponse getSurveyMeta();

    OnboardingSurveyResponse submitSurvey(Long userId, OnboardingSurveyRequest request);
}
