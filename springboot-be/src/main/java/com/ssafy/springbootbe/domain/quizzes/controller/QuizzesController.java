package com.ssafy.springbootbe.domain.quizzes.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;
import com.ssafy.springbootbe.domain.quizzes.service.QuizzesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizzesController {
    private final QuizzesService quizzesService;

    @PostMapping("/sessions")
    public ResponseEntity<QuizSessionStartResponse> startSession(
            @AuthenticationPrincipal LoginUserPrincipal loginUserPrincipal,
            @Valid @RequestBody QuizSessionStartRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(quizzesService.startSession(loginUserPrincipal.getUserId(), request));
    }
}
