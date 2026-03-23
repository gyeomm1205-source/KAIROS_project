package com.ssafy.springbootbe.domain.curricula.exception;

public class CurriculumAlreadyActiveException extends RuntimeException {

    public CurriculumAlreadyActiveException(Long userId) {
        super("이미 활성화된 커리큘럼이 존재합니다. userId=" + userId);
    }
}