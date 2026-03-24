package com.ssafy.springbootbe.domain.curricula.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumConfirmRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumNodeUpdateRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumPreviewRequest;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumConfirmResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumPreviewResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.service.CurriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/curricula")
@RequiredArgsConstructor
public class CurriculaController {

    private final CurriculaService curriculaService;

    @PostMapping("/preview")
    public ResponseEntity<CurriculumPreviewResponse> preview(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody CurriculumPreviewRequest request) {
        return ResponseEntity.ok(curriculaService.preview(principal.getUserId(), request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<CurriculumConfirmResponse> confirm(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @Valid @RequestBody CurriculumConfirmRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(curriculaService.confirm(principal.getUserId(), request));
    }

    @PatchMapping("/nodes/{nodeId}")
    public ResponseEntity<CurriculumNodeResponse> updateNode(
            @AuthenticationPrincipal LoginUserPrincipal principal,
            @PathVariable Long nodeId,
            @RequestBody CurriculumNodeUpdateRequest request) {
        return ResponseEntity.ok(curriculaService.updateNode(principal.getUserId(), nodeId, request));
    }

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