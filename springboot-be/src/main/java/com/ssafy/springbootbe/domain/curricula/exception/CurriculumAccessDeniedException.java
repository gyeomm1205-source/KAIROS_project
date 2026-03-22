package com.ssafy.springbootbe.domain.curricula.exception;

public class CurriculumAccessDeniedException extends RuntimeException {

    public CurriculumAccessDeniedException(Long curriculumId) {
        super("해당 커리큘럼에 접근 권한이 없습니다. curriculumId=" + curriculumId);
    }
}