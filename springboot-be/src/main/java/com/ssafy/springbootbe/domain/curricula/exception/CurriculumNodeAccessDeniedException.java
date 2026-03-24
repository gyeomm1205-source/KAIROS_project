package com.ssafy.springbootbe.domain.curricula.exception;

public class CurriculumNodeAccessDeniedException extends RuntimeException {

    public CurriculumNodeAccessDeniedException(Long curriculumNodeId) {
        super("해당 커리큘럼 노드에 접근 권한이 없습니다. curriculumNodeId=" + curriculumNodeId);
    }
}