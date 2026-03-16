package com.ssafy.springbootbe.domain.curricula.controller;

import com.ssafy.springbootbe.domain.curricula.service.CurriculaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/curricula")
@RequiredArgsConstructor
public class CurriculaController {
    private final CurriculaService curriculaService;
}
