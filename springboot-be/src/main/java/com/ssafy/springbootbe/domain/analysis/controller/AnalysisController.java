package com.ssafy.springbootbe.domain.analysis.controller;

import com.ssafy.springbootbe.domain.analysis.dto.request.AnalysisCompleteRequest;
import com.ssafy.springbootbe.domain.analysis.dto.response.AnalysisCompleteResponse;
import com.ssafy.springbootbe.domain.analysis.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/complete")
    public ResponseEntity<AnalysisCompleteResponse> complete(@RequestBody AnalysisCompleteRequest request) {
        return ResponseEntity.ok(analysisService.complete(request));
    }
}
