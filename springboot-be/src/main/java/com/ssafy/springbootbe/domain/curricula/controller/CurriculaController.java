package com.ssafy.springbootbe.domain.curricula.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.service.CurriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/curricula")
@RequiredArgsConstructor
public class CurriculaController {

    private final CurriculaService curriculaService;

    @GetMapping("/{curriculumId}/reason")
    public ResponseEntity<CurriculumReasonResponse> getReason(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long curriculumId) {
        return ResponseEntity.ok(curriculaService.getReason(principal.getUserId(), curriculumId));
    }

    @GetMapping("/nodes/{nodeId}")
    public ResponseEntity<CurriculumNodeResponse> getNode(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long nodeId) {
        return ResponseEntity.ok(curriculaService.getNode(principal.getUserId(), nodeId));
    }
}