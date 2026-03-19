package com.ssafy.springbootbe.domain.activities.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.service.ActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivitiesController {

    private final ActivitiesService activitiesService;

    @GetMapping
    public ResponseEntity<ActivityPageResponse> getActivities(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(activitiesService.findActivities(principal.getUserId(), year, month, sort, page, size));
    }

    @PatchMapping("/{activityId}/inclusion")
    public ResponseEntity<ActivityInclusionResponse> updateInclusion(
            @PathVariable Long activityId,
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestBody ActivityInclusionRequest request) {
        return ResponseEntity.ok(activitiesService.updateInclusion(principal.getUserId(), activityId, request));
    }
}
