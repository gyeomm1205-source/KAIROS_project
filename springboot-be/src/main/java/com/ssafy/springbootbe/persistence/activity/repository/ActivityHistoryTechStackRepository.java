package com.ssafy.springbootbe.persistence.activity.repository;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityHistoryTechStackRepository extends JpaRepository<ActivityHistoryTechStack, Long> {

    List<ActivityHistoryTechStack> findByActivityHistoryActivityHistoryId(Long activityHistoryId);

    void deleteByActivityHistoryActivityHistoryId(Long activityHistoryId);
}
