package com.ssafy.springbootbe.domain.recommendations.controller;

import com.ssafy.springbootbe.domain.recommendations.service.RecommendationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationsService recommendationsService;
}
