package com.ssafy.springbootbe.domain.recommendations.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListResponse;
import com.ssafy.springbootbe.domain.recommendations.service.RecommendationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationsService recommendationsService;

    @GetMapping
    public ResponseEntity<RecommendationListResponse> getRecommendations(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal) {
        return ResponseEntity.ok(recommendationsService.findRecommendations(loginUserPrincipal.getUserId()));
    }
}
