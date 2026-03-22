package com.ssafy.springbootbe.domain.curricula.exception;

public class CurriculumNotFoundException extends RuntimeException {

    public CurriculumNotFoundException(Long curriculumId) {
        super("커리큘럼을 찾을 수 없습니다. curriculumId=" + curriculumId);
    }
}
