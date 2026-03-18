package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.response.ActivityHistoryResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivitiesServiceImpl implements ActivitiesService {

    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;

    @Override
    @Transactional(readOnly = true)
    public ActivityPageResponse findActivities(Long userId, Integer year, Integer month, String sort, int page, int size) {
        Sort.Direction direction = "oldest".equalsIgnoreCase(sort) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(direction, "activityDate"));

        Page<ActivityHistory> activityPage;
        if (year != null && month != null) {
            activityPage = activityHistoryRepository.findByUserUserIdAndYearAndMonth(userId, year, month, pageable);
        } else if (year != null) {
            activityPage = activityHistoryRepository.findByUserUserIdAndYear(userId, year, pageable);
        } else {
            activityPage = activityHistoryRepository.findByUserUserId(userId, pageable);
        }

        List<ActivityHistoryResponse> items = activityPage.getContent().stream()
                .map(activity -> {
                    List<TechStackInfo> techStacks = activityHistoryTechStackRepository
                            .findByActivityHistoryActivityHistoryId(activity.getActivityHistoryId())
                            .stream()
                            .map(ahts -> TechStackInfo.from(ahts.getTechStack()))
                            .toList();
                    return ActivityHistoryResponse.from(activity, techStacks);
                })
                .toList();

        log.info("활동 이력 조회. userId={}, year={}, month={}, total={}", userId, year, month, activityPage.getTotalElements());

        return ActivityPageResponse.builder()
                .total(activityPage.getTotalElements())
                .page(page)
                .size(size)
                .items(items)
                .build();
    }
}
