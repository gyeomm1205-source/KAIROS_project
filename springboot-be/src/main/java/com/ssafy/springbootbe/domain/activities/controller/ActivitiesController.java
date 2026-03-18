package com.ssafy.springbootbe.domain.activities.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.service.ActivitiesService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivitiesController {

    private final ActivitiesService activitiesService;
    private final JWTUtils jwtUtils;

    @GetMapping
    public ResponseEntity<ActivityPageResponse> getActivities(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = extractUserId(authorizationHeader);
        return ResponseEntity.ok(activitiesService.findActivities(userId, year, month, sort, page, size));
    }

    private Long extractUserId(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        Claims claims = jwtUtils.getClaims(token);
        return ((Number) claims.get("userId")).longValue();
    }
}
