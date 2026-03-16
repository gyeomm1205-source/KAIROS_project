package com.ssafy.springbootbe.persistence.user.repository;

import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserTechStackRepository extends JpaRepository<UserTechStack, Long> {

    List<UserTechStack> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);
}
