package com.ssafy.springbootbe.domain.schedules.controller;

import com.ssafy.springbootbe.domain.schedules.service.SchedulesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/schedules")
@RequiredArgsConstructor
public class SchedulesController {
    private final SchedulesService schedulesService;
}
