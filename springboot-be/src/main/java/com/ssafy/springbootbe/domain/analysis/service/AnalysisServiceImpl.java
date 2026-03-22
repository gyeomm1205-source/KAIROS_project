package com.ssafy.springbootbe.domain.analysis.service;

import com.ssafy.springbootbe.domain.analysis.dto.request.AnalysisCompleteRequest;
import com.ssafy.springbootbe.domain.analysis.dto.response.AnalysisCompleteResponse;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final UserRepository userRepository;
    private final TechStackRepository techStackRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final ActivityHistoryRepository activityHistoryRepository;
    private final ActivityHistoryTechStackRepository activityHistoryTechStackRepository;

    @Override
    @Transactional
    public AnalysisCompleteResponse complete(AnalysisCompleteRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 userId=" + request.getUserId()));

        updateTechScores(user, request.getTechScores());

        int savedCount = saveActivities(user, request.getGithubActivities())
                + saveActivities(user, request.getVelogActivities());

        log.info("분석 완료 콜백 처리. userId={}, taskId={}, savedCount={}", request.getUserId(), request.getTaskId(), savedCount);

        return AnalysisCompleteResponse.builder()
                .message("activity_history saved.")
                .savedCount(savedCount)
                .build();
    }

    private void updateTechScores(User user, List<AnalysisCompleteRequest.TechScoreItem> techScores) {
        if (techScores == null) {
            return;
        }
        for (AnalysisCompleteRequest.TechScoreItem item : techScores) {
            Optional<TechStack> techStackOpt = techStackRepository.findByTechName(item.getTechName());
            if (techStackOpt.isEmpty()) {
                log.warn("기술 스택을 찾을 수 없어 score 업데이트 생략. techName={}", item.getTechName());
                continue;
            }
            TechStack techStack = techStackOpt.get();
            Optional<UserTechStack> userTechStackOpt = userTechStackRepository
                    .findByUserUserIdAndTechStackTechStackId(user.getUserId(), techStack.getTechStackId());

            if (userTechStackOpt.isPresent()) {
                userTechStackOpt.get().updateScore(item.getScore());
            } else {
                userTechStackRepository.save(UserTechStack.builder()
                        .user(user)
                        .techStack(techStack)
                        .score(item.getScore())
                        .build());
            }
        }
    }

    private int saveActivities(User user, List<AnalysisCompleteRequest.ActivityItem> activities) {
        if (activities == null) {
            return 0;
        }
        int savedCount = 0;
        for (AnalysisCompleteRequest.ActivityItem item : activities) {
            ActivityType activityType;
            try {
                activityType = ActivityType.valueOf(item.getActivityType());
            } catch (IllegalArgumentException e) {
                log.warn("지원하지 않는 activityType 무시. activityType={}", item.getActivityType());
                continue;
            }

            ActivityHistory activity = ActivityHistory.builder()
                    .user(user)
                    .activityType(activityType)
                    .title(item.getTitle())
                    .description(item.getDescription())
                    .activityDate(item.getActivityDate())
                    .build();
            activityHistoryRepository.save(activity);

            if (item.getTechStacks() != null) {
                List<ActivityHistoryTechStack> techStackLinks = new ArrayList<>();
                for (String techName : item.getTechStacks()) {
                    techStackRepository.findByTechName(techName).ifPresentOrElse(
                            techStack -> techStackLinks.add(ActivityHistoryTechStack.builder()
                                    .activityHistory(activity)
                                    .techStack(techStack)
                                    .build()),
                            () -> log.warn("기술 스택을 찾을 수 없어 연결 생략. techName={}", techName)
                    );
                }
                activityHistoryTechStackRepository.saveAll(techStackLinks);
            }
            savedCount++;
        }
        return savedCount;
    }
}
