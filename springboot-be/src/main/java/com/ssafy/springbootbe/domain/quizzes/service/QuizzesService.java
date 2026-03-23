package com.ssafy.springbootbe.domain.quizzes.service;

import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;

public interface QuizzesService {
    QuizSessionStartResponse startSession(Long userId, QuizSessionStartRequest request);
}
