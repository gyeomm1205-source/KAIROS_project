package com.ssafy.springbootbe.persistence.user.repository;

import com.ssafy.springbootbe.persistence.user.entity.UserDesiredPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserDesiredPositionRepository extends JpaRepository<UserDesiredPosition, Long> {

    List<UserDesiredPosition> findByUserUserId(Long userId);

    void deleteByUserUserId(Long userId);
}
