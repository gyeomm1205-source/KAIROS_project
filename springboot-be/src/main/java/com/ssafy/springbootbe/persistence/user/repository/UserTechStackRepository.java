package com.ssafy.springbootbe.persistence.user.repository;

import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {

    List<UserTechStack> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);

    Optional<UserTechStack> findByUserUserIdAndTechStackTechStackId(Long userId, Long techStackId);

    List<UserTechStack> findTop6ByUserUserIdOrderByScoreDesc(Long userId);

    List<UserTechStack> findByTechStackTechStackIdOrderByScoreDesc(Long techStackId);

    long countByTechStackTechStackId(Long techStackId);

    long countByTechStackTechStackIdAndScoreGreaterThan(Long techStackId, Double score);
}
