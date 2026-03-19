package com.ssafy.springbootbe.domain.schedules.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;
import com.ssafy.springbootbe.domain.schedules.service.SchedulesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class SchedulesController {

    private final SchedulesService schedulesService;

    @PostMapping
    public ResponseEntity<ScheduleResponse> createSchedule(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody ScheduleCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schedulesService.createSchedule(principal.getUserId(), request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ScheduleResponse> updateSchedule(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id,
            @RequestBody ScheduleUpdateRequest request) {
        return ResponseEntity.ok(schedulesService.updateSchedule(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSchedule(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long id) {
        schedulesService.deleteSchedule(principal.getUserId(), id);
        return ResponseEntity.noContent().build();
    }
}
