package com.ssafy.springbootbe.persistence.reference.repository;

import com.ssafy.springbootbe.persistence.reference.entity.ReferenceTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferenceTechStackRepository extends JpaRepository<ReferenceTechStack, Long> {

    // 기술 스택 기반 레퍼런스 조회 (idx_reference_tech_stack)
    List<ReferenceTechStack> findByTechStackTechStackId(Long techStackId);
}
