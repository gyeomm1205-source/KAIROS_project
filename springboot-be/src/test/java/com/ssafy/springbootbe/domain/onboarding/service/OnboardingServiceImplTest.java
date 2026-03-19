package com.ssafy.springbootbe.domain.onboarding.service;

import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingMetaResponse;
import com.ssafy.springbootbe.domain.onboarding.dto.request.OnboardingSurveyRequest;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingSurveyResponse;
import com.ssafy.springbootbe.domain.onboarding.exception.AnalysisReportPreparationException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingAccessDeniedException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingMetaRetrievalException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingReferenceNotFoundException;
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
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private DevPositionRepository devPositionRepository;

    @Mock
    private TechStackRepository techStackRepository;

    @Mock
    private UserDesiredPositionRepository userDesiredPositionRepository;

    @Mock
    private UserTechStackRepository userTechStackRepository;

    @Mock
    private UserCurriculumCategoryRepository userCurriculumCategoryRepository;

    @Mock
    private AnalysisReportRepository analysisReportRepository;

    @InjectMocks
    private OnboardingServiceImpl onboardingService;

    private User guestUser;
    private OnboardingSurveyRequest validRequest;

    @BeforeEach
    void setUp() {
        guestUser = User.builder()
                .userId(1L)
                .email("guest@gmail.com")
                .nickname("guest")
                .status(UserStatus.GUEST)
                .calendarSyncEnabled(false)
                .build();

        validRequest = new OnboardingSurveyRequest(
                UserPosition.STUDENT,
                List.of(1L, 3L),
                List.of(2L, 5L, 7L),
                List.of(CurriculumCategory.THEORY, CurriculumCategory.PRACTICE)
        );
    }

    @Test
    void getSurveyMeta_성공_기술스택과_포지션_목록을_반환한다() {
        // given
        given(techStackRepository.findAllByOrderByTechNameAsc()).willReturn(List.of(
                TechStack.builder().techStackId(2L).techName("Java").iconUrl("java.png").color("#007396").build(),
                TechStack.builder().techStackId(5L).techName("Spring").iconUrl("spring.png").color("#6DB33F").build()
        ));
        given(devPositionRepository.findAllByOrderByPositionNameAsc()).willReturn(List.of(
                DevPosition.builder().devPositionId(1L).positionName("Backend").build(),
                DevPosition.builder().devPositionId(3L).positionName("Data Engineer").build()
        ));

        // when
        OnboardingMetaResponse response = onboardingService.getSurveyMeta();

        // then
        assertThat(response.getTechStacks()).hasSize(2);
        assertThat(response.getTechStacks())
                .extracting("techStackId", "techName", "iconUrl", "color")
                .containsExactly(
                        tuple(2L, "Java", "java.png", "#007396"),
                        tuple(5L, "Spring", "spring.png", "#6DB33F")
                );
        assertThat(response.getDevPositions()).hasSize(2);
        assertThat(response.getDevPositions())
                .extracting("devPositionId", "positionName")
                .containsExactly(
                        tuple(1L, "Backend"),
                        tuple(3L, "Data Engineer")
                );
    }

    @Test
    void getSurveyMeta_실패_조회중_예외가_발생하면_도메인예외로_감싼다() {
        // given
        given(techStackRepository.findAllByOrderByTechNameAsc()).willThrow(new RuntimeException("db error"));

        // when & then
        assertThatThrownBy(() -> onboardingService.getSurveyMeta())
                .isInstanceOf(OnboardingMetaRetrievalException.class)
                .hasMessageContaining("온보딩 메타데이터 조회에 실패했습니다.");
    }

    @Test
    void submitSurvey_성공_GUEST를_SURVEYED로_변경하고_분석리포트를_생성한다() {
        // given
        DevPosition backend = DevPosition.builder().devPositionId(1L).positionName("Backend").build();
        DevPosition ai = DevPosition.builder().devPositionId(3L).positionName("AI").build();
        TechStack java = TechStack.builder().techStackId(2L).techName("Java").build();
        TechStack spring = TechStack.builder().techStackId(5L).techName("Spring").build();
        TechStack redis = TechStack.builder().techStackId(7L).techName("Redis").build();
        AnalysisReport analysisReport = AnalysisReport.builder()
                .analysisReportId(42L)
                .user(guestUser)
                .status(AnalysisStatus.PENDING)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(validRequest.getDesiredPositionIds())).willReturn(List.of(backend, ai));
        given(techStackRepository.findAllById(validRequest.getTechStackIds())).willReturn(List.of(java, spring, redis));
        given(analysisReportRepository.saveAndFlush(any(AnalysisReport.class))).willReturn(analysisReport);

        // when
        OnboardingSurveyResponse response = onboardingService.submitSurvey(1L, validRequest);

        // then
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(UserStatus.SURVEYED);
        assertThat(response.getConsiderPersonalSchedule()).isTrue();
        assertThat(response.getAnalysisReportId()).isEqualTo(42L);
        assertThat(response.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
        assertThat(guestUser.getPosition()).isEqualTo(UserPosition.STUDENT);
        assertThat(guestUser.getStatus()).isEqualTo(UserStatus.SURVEYED);
        assertThat(guestUser.getCalendarSyncEnabled()).isTrue();
        verify(userDesiredPositionRepository).deleteByUserUserId(1L);
        verify(userTechStackRepository).deleteByUserUserId(1L);
        verify(userCurriculumCategoryRepository).deleteByUserUserId(1L);
    }

    @Test
    void submitSurvey_실패_GUEST가_아니면_ACCESS_DENIED() {
        // given
        User surveyedUser = User.builder()
                .userId(1L)
                .status(UserStatus.SURVEYED)
                .build();
        given(userRepository.findById(1L)).willReturn(Optional.of(surveyedUser));

        // when & then
        assertThatThrownBy(() -> onboardingService.submitSurvey(1L, validRequest))
                .isInstanceOf(OnboardingAccessDeniedException.class);
    }

    @Test
    void submitSurvey_실패_존재하지_않는_techStackId가_있으면_NOT_FOUND() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(validRequest.getDesiredPositionIds())).willReturn(List.of(
                DevPosition.builder().devPositionId(1L).build(),
                DevPosition.builder().devPositionId(3L).build()
        ));
        given(techStackRepository.findAllById(validRequest.getTechStackIds())).willReturn(List.of(
                TechStack.builder().techStackId(2L).build(),
                TechStack.builder().techStackId(5L).build()
        ));

        // when & then
        assertThatThrownBy(() -> onboardingService.submitSurvey(1L, validRequest))
                .isInstanceOf(OnboardingReferenceNotFoundException.class)
                .hasMessageContaining("techStackId");
    }

    @Test
    void submitSurvey_실패_존재하지_않는_devPositionId가_있으면_NOT_FOUND() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(validRequest.getDesiredPositionIds())).willReturn(List.of(
                DevPosition.builder().devPositionId(1L).build()
        ));

        // when & then
        assertThatThrownBy(() -> onboardingService.submitSurvey(1L, validRequest))
                .isInstanceOf(OnboardingReferenceNotFoundException.class)
                .hasMessageContaining("devPositionId");
    }

    @Test
    void submitSurvey_연관테이블을_전체_교체_방식으로_저장한다() {
        // given
        DevPosition backend = DevPosition.builder().devPositionId(1L).positionName("Backend").build();
        DevPosition ai = DevPosition.builder().devPositionId(3L).positionName("AI").build();
        TechStack java = TechStack.builder().techStackId(2L).techName("Java").build();
        TechStack spring = TechStack.builder().techStackId(5L).techName("Spring").build();
        TechStack redis = TechStack.builder().techStackId(7L).techName("Redis").build();

        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(validRequest.getDesiredPositionIds())).willReturn(List.of(backend, ai));
        given(techStackRepository.findAllById(validRequest.getTechStackIds())).willReturn(List.of(java, spring, redis));
        given(analysisReportRepository.saveAndFlush(any(AnalysisReport.class))).willReturn(AnalysisReport.builder()
                .analysisReportId(42L)
                .user(guestUser)
                .status(AnalysisStatus.PENDING)
                .build());

        // when
        onboardingService.submitSurvey(1L, validRequest);

        // then
        ArgumentCaptor<List<UserDesiredPosition>> desiredPositionCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<UserTechStack>> techStackCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<UserCurriculumCategory>> categoryCaptor = ArgumentCaptor.forClass(List.class);

        verify(userDesiredPositionRepository).saveAll(desiredPositionCaptor.capture());
        verify(userTechStackRepository).saveAll(techStackCaptor.capture());
        verify(userCurriculumCategoryRepository).saveAll(categoryCaptor.capture());

        assertThat(desiredPositionCaptor.getValue()).hasSize(2);
        assertThat(desiredPositionCaptor.getValue())
                .extracting(item -> item.getDevPosition().getDevPositionId())
                .containsExactly(1L, 3L);
        assertThat(techStackCaptor.getValue()).hasSize(3);
        assertThat(techStackCaptor.getValue())
                .extracting(item -> item.getTechStack().getTechStackId())
                .containsExactly(2L, 5L, 7L);
        assertThat(categoryCaptor.getValue()).hasSize(2);
        assertThat(categoryCaptor.getValue())
                .extracting(UserCurriculumCategory::getCategory)
                .containsExactly(CurriculumCategory.THEORY, CurriculumCategory.PRACTICE);
    }

    @Test
    void submitSurvey_분석리포트_생성에_실패하면_예외를_던진다() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(validRequest.getDesiredPositionIds())).willReturn(List.of(
                DevPosition.builder().devPositionId(1L).build(),
                DevPosition.builder().devPositionId(3L).build()
        ));
        given(techStackRepository.findAllById(validRequest.getTechStackIds())).willReturn(List.of(
                TechStack.builder().techStackId(2L).build(),
                TechStack.builder().techStackId(5L).build(),
                TechStack.builder().techStackId(7L).build()
        ));
        given(analysisReportRepository.saveAndFlush(any(AnalysisReport.class)))
                .willThrow(new RuntimeException("db error"));

        // when & then
        assertThatThrownBy(() -> onboardingService.submitSurvey(1L, validRequest))
                .isInstanceOf(AnalysisReportPreparationException.class);
    }

    @Test
    void submitSurvey_중복_입력은_제거한_뒤_저장한다() {
        // given
        OnboardingSurveyRequest duplicateRequest = new OnboardingSurveyRequest(
                UserPosition.STUDENT,
                List.of(1L, 1L, 3L),
                List.of(2L, 2L, 5L),
                List.of(CurriculumCategory.THEORY, CurriculumCategory.THEORY, CurriculumCategory.PRACTICE)
        );

        given(userRepository.findById(1L)).willReturn(Optional.of(guestUser));
        given(devPositionRepository.findAllById(eq(List.of(1L, 3L)))).willReturn(List.of(
                DevPosition.builder().devPositionId(1L).build(),
                DevPosition.builder().devPositionId(3L).build()
        ));
        given(techStackRepository.findAllById(eq(List.of(2L, 5L)))).willReturn(List.of(
                TechStack.builder().techStackId(2L).build(),
                TechStack.builder().techStackId(5L).build()
        ));
        given(analysisReportRepository.saveAndFlush(any(AnalysisReport.class))).willReturn(AnalysisReport.builder()
                .analysisReportId(42L)
                .user(guestUser)
                .status(AnalysisStatus.PENDING)
                .build());

        // when
        onboardingService.submitSurvey(1L, duplicateRequest);

        // then
        verify(devPositionRepository).findAllById(eq(List.of(1L, 3L)));
        verify(techStackRepository).findAllById(eq(List.of(2L, 5L)));
    }
}
