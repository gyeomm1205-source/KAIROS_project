package com.ssafy.springbootbe.domain.curricula.service;

import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeNotFoundException;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumRecommendationReason;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRecommendationReasonRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CurriculaServiceImpl implements CurriculaService {

    private final CurriculumRepository curriculumRepository;
    private final CurriculumNodeRepository curriculumNodeRepository;
    private final CurriculumRecommendationReasonRepository curriculumRecommendationReasonRepository;

    @Override
    @Transactional(readOnly = true)
    public CurriculumReasonResponse getReason(Long userId, Long curriculumId) {
        Curriculum curriculum = curriculumRepository.findById(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

        if (!curriculum.getUser().getUserId().equals(userId)) {
            throw new CurriculumAccessDeniedException(curriculumId);
        }

        CurriculumRecommendationReason reason = curriculumRecommendationReasonRepository
                .findByCurriculumCurriculumId(curriculumId)
                .orElseThrow(() -> new CurriculumNotFoundException(curriculumId));

        log.info("커리큘럼 AI 추천 근거 조회. userId={}, curriculumId={}", userId, curriculumId);

        return CurriculumReasonResponse.from(reason);
    }

    @Override
    @Transactional(readOnly = true)
    public CurriculumNodeResponse getNode(Long userId, Long curriculumNodeId) {
        CurriculumNode node = curriculumNodeRepository.findById(curriculumNodeId)
                .orElseThrow(() -> new CurriculumNodeNotFoundException(curriculumNodeId));

        if (!node.getCurriculum().getUser().getUserId().equals(userId)) {
            throw new CurriculumNodeAccessDeniedException(curriculumNodeId);
        }

        log.info("커리큘럼 노드 상세 조회. userId={}, curriculumNodeId={}", userId, curriculumNodeId);

        return CurriculumNodeResponse.from(node);
    }
}