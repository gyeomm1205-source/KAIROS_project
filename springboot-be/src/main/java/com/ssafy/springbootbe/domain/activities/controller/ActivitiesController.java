package com.ssafy.springbootbe.domain.activities.controller;

import com.ssafy.springbootbe.domain.activities.service.ActivitiesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/activities")
@RequiredArgsConstructor
public class ActivitiesController {
    private final ActivitiesService activitiesService;
}
