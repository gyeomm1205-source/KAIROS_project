package com.ssafy.springbootbe.domain.calendar.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarExportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarImportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.service.CalendarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/calendar")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping
    public ResponseEntity<CalendarResponse> getCalendar(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @RequestParam int year,
            @RequestParam int month) {
        return ResponseEntity.ok(calendarService.getCalendar(principal.getUserId(), year, month));
    }

    @PostMapping("/sync/export")
    public ResponseEntity<CalendarExportResponse> exportToGoogle(
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ResponseEntity.ok(calendarService.export(principal.getUserId()));
    }

    @PostMapping("/sync/import")
    public ResponseEntity<CalendarImportResponse> importFromGoogle(
            @AuthenticationPrincipal LoginUserPrincipal principal) {
        return ResponseEntity.ok(calendarService.importFromGoogle(principal.getUserId()));
    }
}
