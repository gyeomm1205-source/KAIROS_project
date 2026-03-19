package com.ssafy.springbootbe.domain.schedules.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;
import com.ssafy.springbootbe.domain.schedules.service.SchedulesService;
import io.jsonwebtoken.Claims;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class SchedulesController {

    private final SchedulesService schedulesService;
    private final JWTUtils jwtUtils;

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody ScheduleCreateRequest request) {
        Long userId = extractUserId(authorizationHeader);
        ScheduleResponse response = schedulesService.createSchedule(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id,
            @RequestBody ScheduleUpdateRequest request) {
        Long userId = extractUserId(authorizationHeader);
        ScheduleResponse response = schedulesService.updateSchedule(userId, id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @RequestHeader("Authorization") String authorizationHeader,
            @PathVariable Long id) {
        Long userId = extractUserId(authorizationHeader);
        schedulesService.deleteSchedule(userId, id);
        return ResponseEntity.noContent().build();
    }

    private Long extractUserId(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        Claims claims = jwtUtils.getClaims(token);
        return ((Number) claims.get("userId")).longValue();
    }
}
