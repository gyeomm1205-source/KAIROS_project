package com.ssafy.springbootbe.domain.activities.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivitySyncResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.GrowthReportResponse;
import com.ssafy.springbootbe.domain.activities.service.ActivitiesService;
import com.ssafy.springbootbe.domain.activities.service.ActivitySyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivitiesController {

    private final ActivitiesService activitiesService;
    private final ActivitySyncService activitySyncService;

    @GetMapping("/growth-report")
    public ResponseEntity<GrowthReportResponse> getGrowthReport(
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ResponseEntity.ok(activitiesService.getGrowthReport(principal.getUserId()));
    }

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

    @PostMapping("/sync/{provider}")
    public ResponseEntity<ActivitySyncResponse> syncActivities(
            @PathVariable String provider,
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ResponseEntity.ok(activitySyncService.syncActivities(principal.getUserId(), provider));
    }
}
