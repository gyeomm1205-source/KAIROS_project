package com.ssafy.springbootbe.domain.onboarding.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingMetaResponse;
import com.ssafy.springbootbe.domain.onboarding.dto.request.OnboardingSurveyRequest;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingSurveyResponse;
import com.ssafy.springbootbe.domain.onboarding.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/onboarding")
@RequiredArgsConstructor
public class OnboardingController {
    private final OnboardingService onboardingService;

    @GetMapping("/meta")
    public ResponseEntity<OnboardingMetaResponse> getSurveyMeta(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return ResponseEntity.ok(onboardingService.getSurveyMeta());
    }

    @PostMapping("/survey")
    public ResponseEntity<OnboardingSurveyResponse> submitSurvey(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody OnboardingSurveyRequest request) {
        return ResponseEntity.ok(onboardingService.submitSurvey(loginUserPrincipal.getUserId(), request));
    }
}
