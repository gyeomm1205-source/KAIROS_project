package com.ssafy.springbootbe.domain.test;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;

    @GetMapping("/tstGet")
    public Map<String, Object> tstGet() {
        return testService.tstGet();
    }

    @PostMapping("/tstPost")
    public String tstPost() {
        return testService.tstPost();
    }
}
