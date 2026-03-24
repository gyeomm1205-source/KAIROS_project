package com.ssafy.springbootbe.persistence.techstack.repository;

import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {

    // 온보딩 메타 조회 시 전체 기술 스택 목록
    List<TechStack> findAllByOrderByTechNameAsc();

    Optional<TechStack> findByTechName(String techName);
}
