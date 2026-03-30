package com.ssafy.springbootbe.persistence.curriculum.repository;

import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CurriculumTechStackRepository extends JpaRepository<CurriculumTechStack, Long> {

    List<CurriculumTechStack> findByCurriculumCurriculumId(Long curriculumId);

    List<CurriculumTechStack> findByCurriculumCurriculumIdIn(List<Long> curriculumIds);
}
