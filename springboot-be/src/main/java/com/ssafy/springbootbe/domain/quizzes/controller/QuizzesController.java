package com.ssafy.springbootbe.domain.quizzes.controller;

import com.ssafy.springbootbe.domain.quizzes.service.QuizzesService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/quizzes")
@RequiredArgsConstructor
public class QuizzesController {
    private final QuizzesService quizzesService;
}
