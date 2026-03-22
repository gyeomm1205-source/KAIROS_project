package com.ssafy.springbootbe.domain.curricula.exception;

public class CurriculumNodeNotFoundException extends RuntimeException {

    public CurriculumNodeNotFoundException(Long curriculumNodeId) {
        super("커리큘럼 노드를 찾을 수 없습니다. curriculumNodeId=" + curriculumNodeId);
    }
}