package com.ssafy.springbootbe.domain.curricula.service;

import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;

public interface CurriculaService {

    CurriculumReasonResponse getReason(Long userId, Long curriculumId);

    CurriculumNodeResponse getNode(Long userId, Long curriculumNodeId);
}