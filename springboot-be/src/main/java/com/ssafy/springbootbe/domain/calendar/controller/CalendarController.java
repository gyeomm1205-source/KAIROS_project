package com.ssafy.springbootbe.domain.calendar.controller;

import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.service.CalendarService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final JWTUtils jwtUtils;

    @GetMapping
    public ResponseEntity<CalendarResponse> getCalendar(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestParam int year,
            @RequestParam int month) {
        Long userId = extractUserId(authorizationHeader);
        return ResponseEntity.ok(calendarService.getCalendar(userId, year, month));
    }

    private Long extractUserId(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        Claims claims = jwtUtils.getClaims(token);
        return ((Number) claims.get("userId")).longValue();
    }
}
