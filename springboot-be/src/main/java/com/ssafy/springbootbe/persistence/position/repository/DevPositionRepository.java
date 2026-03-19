package com.ssafy.springbootbe.persistence.position.repository;

import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DevPositionRepository extends JpaRepository<DevPosition, Long> {

    // 온보딩 메타 조회 시 전체 포지션 목록
    List<DevPosition> findAllByOrderByPositionNameAsc();
}
