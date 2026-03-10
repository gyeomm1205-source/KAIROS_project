package com.ssafy.s14p21a506.testping.api;

import com.ssafy.s14p21a506.testping.dto.InfrastructureStatusResponse;
import com.ssafy.s14p21a506.testping.dto.TestPingResponse;
import com.ssafy.s14p21a506.testping.service.TestPingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/ping")
public class TestPingApi implements TestPingApiDoc {

    private final TestPingService testPingService;

    @Override
    @GetMapping
    public ResponseEntity<TestPingResponse> pingSpring() {
        return ResponseEntity.ok(testPingService.pingSpring());
    }

    @Override
    @GetMapping("/fastapi")
    public ResponseEntity<TestPingResponse> pingFastApi() {
        return ResponseEntity.ok(testPingService.pingFastApi());
    }

    @Override
    @GetMapping("/infrastructure")
    public ResponseEntity<InfrastructureStatusResponse> checkInfrastructure() {
        return ResponseEntity.ok(testPingService.checkInfrastructure());
    }
}
