package com.ssafy.springbootbe.domain.curricula.service;

import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumConfirmRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumPreviewRequest;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumConfirmResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumPreviewResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;

public interface CurriculaService {

    CurriculumPreviewResponse preview(Long userId, CurriculumPreviewRequest request);

    CurriculumConfirmResponse confirm(Long userId, CurriculumConfirmRequest request);

    CurriculumReasonResponse getReason(Long userId, Long curriculumId);

    CurriculumNodeResponse getNode(Long userId, Long curriculumNodeId);
}