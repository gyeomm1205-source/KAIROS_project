package com.ssafy.springbootbe.domain.onboarding.service;

import com.ssafy.springbootbe.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingMetaResponse;
import com.ssafy.springbootbe.domain.onboarding.dto.request.OnboardingSurveyRequest;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingSurveyResponse;
import com.ssafy.springbootbe.domain.onboarding.exception.AnalysisReportPreparationException;
import com.ssafy.springbootbe.domain.onboarding.exception.InvalidSurveyInputException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingAccessDeniedException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingMetaRetrievalException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingPersistenceException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingReferenceNotFoundException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingUserNotFoundException;
import com.ssafy.springbootbe.persistence.analysis.entity.AnalysisReport;
import com.ssafy.springbootbe.persistence.analysis.repository.AnalysisReportRepository;
import com.ssafy.springbootbe.persistence.analysis.type.AnalysisStatus;
import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import com.ssafy.springbootbe.persistence.position.repository.DevPositionRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserCurriculumCategory;
import com.ssafy.springbootbe.persistence.user.entity.UserDesiredPosition;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserCurriculumCategoryRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserDesiredPositionRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingServiceImpl implements OnboardingService {

    private static final boolean DEFAULT_CONSIDER_PERSONAL_SCHEDULE = true;

    private final UserRepository userRepository;
    private final DevPositionRepository devPositionRepository;
    private final TechStackRepository techStackRepository;
    private final UserDesiredPositionRepository userDesiredPositionRepository;
    private final UserTechStackRepository userTechStackRepository;
    private final UserCurriculumCategoryRepository userCurriculumCategoryRepository;
    private final AnalysisReportRepository analysisReportRepository;

    @Override
    @Transactional(readOnly = true)
    public OnboardingMetaResponse getSurveyMeta() {
        try {
            List<TechStackInfo> techStacks = techStackRepository.findAllByOrderByTechNameAsc().stream()
                    .map(TechStackInfo::from)
                    .toList();
            List<DevPositionInfo> devPositions = devPositionRepository.findAllByOrderByPositionNameAsc().stream()
                    .map(DevPositionInfo::from)
                    .toList();

            return OnboardingMetaResponse.builder()
                    .techStacks(techStacks)
                    .devPositions(devPositions)
                    .build();
        } catch (RuntimeException e) {
            throw new OnboardingMetaRetrievalException("온보딩 메타데이터 조회에 실패했습니다.", e);
        }
    }

    @Override
    @Transactional
    public OnboardingSurveyResponse submitSurvey(Long userId, OnboardingSurveyRequest request) {
        User user = findUserByIdOrThrow(userId);
        validateGuestUser(user);

        LinkedHashSet<Long> desiredPositionIds = toDistinctIdSet(request.getDesiredPositionIds(), "desiredPositionIds");
        LinkedHashSet<Long> techStackIds = toDistinctIdSet(request.getTechStackIds(), "techStackIds");
        LinkedHashSet<CurriculumCategory> curriculumCategories =
                toDistinctCategorySet(request.getCurriculumCategories());

        List<DevPosition> devPositions = findDevPositionsOrThrow(desiredPositionIds);
        List<TechStack> techStacks = findTechStacksOrThrow(techStackIds);

        user.updateProfile(request.getPosition(), DEFAULT_CONSIDER_PERSONAL_SCHEDULE);
        user.updateStatus(UserStatus.SURVEYED);

        replaceDesiredPositions(user, devPositions);
        replaceTechStacks(user, techStacks);
        replaceCurriculumCategories(user, curriculumCategories);

        AnalysisReport analysisReport = createPendingAnalysisReport(user);

        log.info("온보딩 설문 저장 완료. userId={}, analysisReportId={}", userId, analysisReport.getAnalysisReportId());

        return OnboardingSurveyResponse.builder()
                .userId(user.getUserId())
                .status(user.getStatus())
                .considerPersonalSchedule(user.getCalendarSyncEnabled())
                .analysisReportId(analysisReport.getAnalysisReportId())
                .analysisStatus(analysisReport.getStatus())
                .build();
    }

    private User findUserByIdOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new OnboardingUserNotFoundException(userId));
    }

    private void validateGuestUser(User user) {
        if (user.getStatus() != UserStatus.GUEST) {
            throw new OnboardingAccessDeniedException(user.getUserId(), user.getStatus());
        }
    }

    private LinkedHashSet<Long> toDistinctIdSet(List<Long> values, String fieldName) {
        LinkedHashSet<Long> distinctValues = new LinkedHashSet<>();
        values.forEach(value -> {
            if (value == null) {
                throw new InvalidSurveyInputException(fieldName + "에는 null이 포함될 수 없습니다.");
            }
            distinctValues.add(value);
        });

        if (distinctValues.isEmpty()) {
            throw new InvalidSurveyInputException(fieldName + "는 비어 있을 수 없습니다.");
        }

        return distinctValues;
    }

    private LinkedHashSet<CurriculumCategory> toDistinctCategorySet(List<CurriculumCategory> categories) {
        LinkedHashSet<CurriculumCategory> distinctCategories = new LinkedHashSet<>();
        categories.forEach(category -> {
            if (category == null) {
                throw new InvalidSurveyInputException("curriculumCategories에는 null이 포함될 수 없습니다.");
            }
            distinctCategories.add(category);
        });

        if (distinctCategories.isEmpty()) {
            throw new InvalidSurveyInputException("curriculumCategories는 비어 있을 수 없습니다.");
        }

        return distinctCategories;
    }

    private List<DevPosition> findDevPositionsOrThrow(LinkedHashSet<Long> desiredPositionIds) {
        List<Long> orderedDesiredPositionIds = List.copyOf(desiredPositionIds);
        List<DevPosition> devPositions = devPositionRepository.findAllById(orderedDesiredPositionIds);
        if (devPositions.size() != desiredPositionIds.size()) {
            LinkedHashSet<Long> foundIds = new LinkedHashSet<>();
            devPositions.forEach(devPosition -> foundIds.add(devPosition.getDevPositionId()));

            LinkedHashSet<Long> missingIds = new LinkedHashSet<>(desiredPositionIds);
            missingIds.removeAll(foundIds);
            throw new OnboardingReferenceNotFoundException(
                    "존재하지 않는 devPositionId가 포함되어 있습니다. ids=" + missingIds
            );
        }
        return devPositions;
    }

    private List<TechStack> findTechStacksOrThrow(LinkedHashSet<Long> techStackIds) {
        List<Long> orderedTechStackIds = List.copyOf(techStackIds);
        List<TechStack> techStacks = techStackRepository.findAllById(orderedTechStackIds);
        if (techStacks.size() != techStackIds.size()) {
            LinkedHashSet<Long> foundIds = new LinkedHashSet<>();
            techStacks.forEach(techStack -> foundIds.add(techStack.getTechStackId()));

            LinkedHashSet<Long> missingIds = new LinkedHashSet<>(techStackIds);
            missingIds.removeAll(foundIds);
            throw new OnboardingReferenceNotFoundException(
                    "존재하지 않는 techStackId가 포함되어 있습니다. ids=" + missingIds
            );
        }
        return techStacks;
    }

    private void replaceDesiredPositions(User user, List<DevPosition> devPositions) {
        try {
            userDesiredPositionRepository.saveAll(devPositions.stream()
                    .map(devPosition -> UserDesiredPosition.builder()
                            .user(user)
                            .devPosition(devPosition)
                            .build())
                    .toList());
        } catch (RuntimeException e) {
            throw new OnboardingPersistenceException("희망 포지션 저장에 실패했습니다.", e);
        }
    }

    private void replaceTechStacks(User user, List<TechStack> techStacks) {
        try {
            userTechStackRepository.saveAll(techStacks.stream()
                    .map(techStack -> UserTechStack.builder()
                            .user(user)
                            .techStack(techStack)
                            .build())
                    .toList());
        } catch (RuntimeException e) {
            throw new OnboardingPersistenceException("기술 스택 저장에 실패했습니다.", e);
        }
    }

    private void replaceCurriculumCategories(User user, LinkedHashSet<CurriculumCategory> curriculumCategories) {
        try {
            userCurriculumCategoryRepository.saveAll(curriculumCategories.stream()
                    .map(category -> UserCurriculumCategory.builder()
                            .user(user)
                            .category(category)
                            .build())
                    .toList());
        } catch (RuntimeException e) {
            throw new OnboardingPersistenceException("커리큘럼 카테고리 저장에 실패했습니다.", e);
        }
    }

    private AnalysisReport createPendingAnalysisReport(User user) {
        try {
            return analysisReportRepository.saveAndFlush(AnalysisReport.builder()
                    .user(user)
                    .status(AnalysisStatus.PENDING)
                    .build());
        } catch (RuntimeException e) {
            throw new AnalysisReportPreparationException("analysis report 생성에 실패했습니다.", e);
        }
    }
}
