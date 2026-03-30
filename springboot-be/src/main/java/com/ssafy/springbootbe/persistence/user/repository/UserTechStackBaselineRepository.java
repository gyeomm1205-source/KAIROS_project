package com.ssafy.springbootbe.persistence.user.repository;

import com.ssafy.springbootbe.persistence.user.entity.UserTechStackBaseline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTechStackBaselineRepository extends JpaRepository<UserTechStackBaseline, Long> {

    List<UserTechStackBaseline> findByUserUserId(Long userId);

    boolean existsByUserUserId(Long userId);
}
