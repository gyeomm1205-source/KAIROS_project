package com.ssafy.springbootbe.domain.recommendations.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.AIRestClient;
import com.ssafy.springbootbe.domain.recommendations.dto.request.DailyRecommendationGenerateRequest;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecommendationsServiceImplTest {

    @Mock private CurriculumRepository curriculumRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private ActivityHistoryRepository activityHistoryRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    @Mock private UserScheduleRepository userScheduleRepository;
    @Mock private RedisService redisService;
    @Mock private AIRestClient aiRestClient;

    private RecommendationsServiceImpl recommendationsService;

    private User user;
    private Curriculum curriculum;

    @BeforeEach
    void setUp() {
        recommendationsService = spy(new RecommendationsServiceImpl(
                curriculumRepository,
                userTechStackRepository,
                activityHistoryRepository,
                activityHistoryTechStackRepository,
                userScheduleRepository,
                redisService,
                aiRestClient,
                new ObjectMapper()
        ));

        ReflectionTestUtils.setField(recommendationsService, "aiServerUrl", "http://localhost:8000");
        ReflectionTestUtils.setField(
                recommendationsService,
                "aiRecommendationsDailyGeneratePath",
                "/api/v1/ai/recommendations/daily-generate"
        );

        user = User.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("kairos")
                .position(UserPosition.JOB_SEEKER)
                .considerPersonalSchedule(true)
                .build();

        curriculum = Curriculum.builder()
                .curriculumId(10L)
                .user(user)
                .status(CurriculumStatus.ACTIVE)
                .duration(30)
                .createdAt(LocalDateTime.of(2026, 3, 20, 0, 0))
                .build();
    }

    @Test
    void refreshDailyRecommendations_ACTIVE_커리큘럼만_조회한다() {
        // given
        given(curriculumRepository.findByStatusOrderByCreatedAtDesc(CurriculumStatus.ACTIVE))
                .willReturn(List.of(curriculum));
        doReturn("{\"curriculumId\":10}").when(recommendationsService).requestDailyRecommendationPayload(any());

        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(activityHistoryTechStackRepository.findIncludedTechStackCountsByUserId(1L)).willReturn(List.of());
        given(activityHistoryRepository.findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(1L)).willReturn(List.of());
        given(userScheduleRepository.findTop20ByUserUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(eq(1L), any(LocalDate.class)))
                .willReturn(List.of());
        given(curriculumRepository.findByUserUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(curriculum));

        // when
        recommendationsService.refreshDailyRecommendations();

        // then
        verify(curriculumRepository).findByStatusOrderByCreatedAtDesc(CurriculumStatus.ACTIVE);
        verify(redisService).save(
                "recommendation:1:10",
                "{\"curriculumId\":10}",
                RecommendationsServiceImpl.RECOMMENDATION_TTL_HOURS,
                java.util.concurrent.TimeUnit.HOURS
        );
    }

    @Test
    void buildDailyGenerateRequest_명세_필드명과_값을_구성한다() throws Exception {
        // given
        TechStack java = TechStack.builder().techStackId(1L).techName("Java").build();
        TechStack spring = TechStack.builder().techStackId(2L).techName("Spring").build();
        TechStack redis = TechStack.builder().techStackId(3L).techName("Redis").build();

        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of(
                UserTechStack.builder().user(user).techStack(spring).score(70).build(),
                UserTechStack.builder().user(user).techStack(java).score(80).build()
        ));
        given(activityHistoryTechStackRepository.findIncludedTechStackCountsByUserId(1L))
                .willReturn(List.of(new Object[]{java, 47L}));

        ActivityHistory activity = ActivityHistory.builder()
                .activityHistoryId(100L)
                .user(user)
                .activityType(ActivityType.GITHUB_COMMIT)
                .category(CurriculumCategory.PRACTICE)
                .title("feat: Redis 캐싱 적용")
                .description("user-service repository")
                .activityDate(LocalDateTime.of(2026, 3, 21, 14, 22))
                .isIncluded(true)
                .build();
        given(activityHistoryRepository.findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(1L))
                .willReturn(List.of(activity));
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(100L))
                .willReturn(List.of(
                        ActivityHistoryTechStack.builder().activityHistory(activity).techStack(java).build(),
                        ActivityHistoryTechStack.builder().activityHistory(activity).techStack(redis).build()
                ));

        given(userScheduleRepository.findTop20ByUserUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(eq(1L), any(LocalDate.class)))
                .willReturn(List.of(
                        UserSchedule.builder()
                                .user(user)
                                .title("중간고사")
                                .startDate(LocalDate.of(2026, 4, 20))
                                .endDate(LocalDate.of(2026, 4, 25))
                                .build()
                ));

        Curriculum previousCurriculum = Curriculum.builder()
                .curriculumId(5L)
                .user(user)
                .status(CurriculumStatus.COMPLETED)
                .duration(10)
                .createdAt(LocalDateTime.of(2026, 3, 10, 0, 0))
                .build();
        given(curriculumRepository.findByUserUserIdOrderByCreatedAtDesc(1L))
                .willReturn(List.of(curriculum, previousCurriculum));

        // when
        DailyRecommendationGenerateRequest request = recommendationsService.buildDailyGenerateRequest(curriculum);
        String serialized = new ObjectMapper().writeValueAsString(request);

        // then
        assertThat(request.getUserId()).isEqualTo(1L);
        assertThat(request.getCurriculumId()).isEqualTo(10L);
        assertThat(request.getCurrentLevel()).isEqualTo("MID");
        assertThat(request.getFavoriteTechStacks()).containsExactly("Java", "Spring");
        assertThat(request.getSkillStats()).hasSize(1);
        assertThat(request.getSkillStats().get(0).getSkill()).isEqualTo("Java");
        assertThat(request.getSkillStats().get(0).getCount()).isEqualTo(47L);
        assertThat(request.getRecentActivities()).hasSize(1);
        assertThat(request.getRecentActivities().get(0).getActivityType()).isEqualTo("COMMIT");
        assertThat(request.getRecentActivities().get(0).getCategory()).isEqualTo("PRACTICE");
        assertThat(request.getRecentActivities().get(0).getTechStacks()).containsExactly("Java", "Redis");
        assertThat(request.getGoogleCalendarEvents()).hasSize(1);
        assertThat(request.getRecentCurriculaIds()).containsExactly(5L);
        assertThat(serialized).contains("\"currentLevel\"");
        assertThat(serialized).contains("\"favoriteTechStacks\"");
        assertThat(serialized).contains("\"skillStats\"");
        assertThat(serialized).contains("\"recentActivities\"");
        assertThat(serialized).contains("\"googleCalendarEvents\"");
        assertThat(serialized).contains("\"recentCurriculaIds\"");
    }

    @Test
    void buildDailyGenerateUri_최신_daily_generate_경로를_사용한다() {
        // when
        String uri = recommendationsService.buildDailyGenerateUri();

        // then
        assertThat(uri).isEqualTo("http://localhost:8000/api/v1/ai/recommendations/daily-generate");
    }

    @Test
    void refreshDailyRecommendations_하나_실패해도_다음_커리큘럼을_계속_처리한다() {
        // given
        Curriculum secondCurriculum = Curriculum.builder()
                .curriculumId(11L)
                .user(user)
                .status(CurriculumStatus.ACTIVE)
                .duration(15)
                .createdAt(LocalDateTime.of(2026, 3, 21, 0, 0))
                .build();

        given(curriculumRepository.findByStatusOrderByCreatedAtDesc(CurriculumStatus.ACTIVE))
                .willReturn(List.of(curriculum, secondCurriculum));
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());
        given(activityHistoryTechStackRepository.findIncludedTechStackCountsByUserId(1L)).willReturn(List.of());
        given(activityHistoryRepository.findTop10ByUserUserIdAndIsIncludedTrueOrderByActivityDateDesc(1L)).willReturn(List.of());
        given(userScheduleRepository.findTop20ByUserUserIdAndEndDateGreaterThanEqualOrderByStartDateAsc(eq(1L), any(LocalDate.class)))
                .willReturn(List.of());
        given(curriculumRepository.findByUserUserIdOrderByCreatedAtDesc(1L)).willReturn(List.of(curriculum, secondCurriculum));

        doThrow(new IllegalStateException("first fail"))
                .doReturn("{\"curriculumId\":11}")
                .when(recommendationsService)
                .requestDailyRecommendationPayload(any());

        // when
        recommendationsService.refreshDailyRecommendations();

        // then
        verify(redisService).save(
                "recommendation:1:11",
                "{\"curriculumId\":11}",
                RecommendationsServiceImpl.RECOMMENDATION_TTL_HOURS,
                java.util.concurrent.TimeUnit.HOURS
        );
    }
}
