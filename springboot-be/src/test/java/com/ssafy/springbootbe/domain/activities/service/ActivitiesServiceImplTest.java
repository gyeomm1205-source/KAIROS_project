package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.GrowthReportResponse;
import com.ssafy.springbootbe.domain.activities.exception.ActivityAccessDeniedException;
import com.ssafy.springbootbe.domain.activities.exception.ActivityNotFoundException;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ActivitiesServiceImplTest {

    @Mock private ActivityHistoryRepository activityHistoryRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private LearningStateService learningStateService;

    @InjectMocks
    private ActivitiesServiceImpl activitiesService;

    private User mockUser;
    private User otherUser;
    private ActivityHistory mockActivity;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().userId(1L).email("user@gmail.com").nickname("teddynu").build();
        otherUser = User.builder().userId(2L).email("other@gmail.com").nickname("other").build();
        mockActivity = ActivityHistory.builder()
                .activityHistoryId(10L)
                .user(mockUser)
                .activityType(ActivityType.GITHUB_COMMIT)
                .title("커밋 제목")
                .activityDate(LocalDateTime.of(2025, 3, 1, 12, 0))
                .build();
    }

    // ===== findActivities =====

    @Test
    void findActivities_필터_없음_전체_조회() {
        // given
        Page<ActivityHistory> page = new PageImpl<>(List.of(mockActivity));
        given(activityHistoryRepository.findByUserUserId(eq(1L), any(Pageable.class))).willReturn(page);
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(10L)).willReturn(List.of());

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, null, null, "latest", 1, 20);

        // then
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(1);
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getTitle()).isEqualTo("커밋 제목");
    }

    @Test
    void findActivities_연도_필터_조회() {
        // given
        Page<ActivityHistory> page = new PageImpl<>(List.of(mockActivity));
        given(activityHistoryRepository.findByUserUserIdAndYear(eq(1L), eq(2025), any(Pageable.class))).willReturn(page);
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(10L)).willReturn(List.of());

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, 2025, null, "latest", 1, 20);

        // then
        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getItems().get(0).getActivityDate().getYear()).isEqualTo(2025);
    }

    @Test
    void findActivities_연도_월_필터_조회() {
        // given
        Page<ActivityHistory> page = new PageImpl<>(List.of(mockActivity));
        given(activityHistoryRepository.findByUserUserIdAndYearAndMonth(eq(1L), eq(2025), eq(3), any(Pageable.class))).willReturn(page);
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(10L)).willReturn(List.of());

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, 2025, 3, "latest", 1, 20);

        // then
        assertThat(response.getTotal()).isEqualTo(1);
    }

    @Test
    void findActivities_오래된순_정렬() {
        // given
        Page<ActivityHistory> page = new PageImpl<>(List.of(mockActivity));
        given(activityHistoryRepository.findByUserUserId(eq(1L), any(Pageable.class))).willReturn(page);
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(anyLong())).willReturn(List.of());

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, null, null, "oldest", 1, 20);

        // then
        assertThat(response.getItems()).hasSize(1);
    }

    @Test
    void findActivities_기술스택_포함_조회() {
        // given
        TechStack techStack = TechStack.builder().techStackId(1L).techName("Java").build();
        ActivityHistoryTechStack ahts = ActivityHistoryTechStack.builder()
                .activityHistory(mockActivity)
                .techStack(techStack)
                .build();

        Page<ActivityHistory> page = new PageImpl<>(List.of(mockActivity));
        given(activityHistoryRepository.findByUserUserId(eq(1L), any(Pageable.class))).willReturn(page);
        given(activityHistoryTechStackRepository.findByActivityHistoryActivityHistoryId(10L)).willReturn(List.of(ahts));

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, null, null, "latest", 1, 20);

        // then
        assertThat(response.getItems().get(0).getTechStacks()).hasSize(1);
        assertThat(response.getItems().get(0).getTechStacks().get(0).getTechName()).isEqualTo("Java");
    }

    @Test
    void findActivities_결과_없음() {
        // given
        given(activityHistoryRepository.findByUserUserId(eq(1L), any(Pageable.class))).willReturn(Page.empty());

        // when
        ActivityPageResponse response = activitiesService.findActivities(1L, null, null, "latest", 1, 20);

        // then
        assertThat(response.getTotal()).isEqualTo(0);
        assertThat(response.getItems()).isEmpty();
    }

    // ===== updateInclusion =====

    @Test
    void updateInclusion_성공_포함_해제() {
        // given
        given(activityHistoryRepository.findById(10L)).willReturn(Optional.of(mockActivity));

        // when
        ActivityInclusionResponse response = activitiesService.updateInclusion(1L, 10L, new ActivityInclusionRequest(false));

        // then
        assertThat(response.getActivityHistoryId()).isEqualTo(10L);
        assertThat(response.getIsIncluded()).isFalse();
    }

    @Test
    void updateInclusion_성공_포함_복원() {
        // given
        ActivityHistory excludedActivity = ActivityHistory.builder()
                .activityHistoryId(10L)
                .user(mockUser)
                .activityType(ActivityType.GITHUB_COMMIT)
                .title("커밋 제목")
                .activityDate(LocalDateTime.now())
                .build();
        excludedActivity.toggleInclusion(false);

        given(activityHistoryRepository.findById(10L)).willReturn(Optional.of(excludedActivity));

        // when
        ActivityInclusionResponse response = activitiesService.updateInclusion(1L, 10L, new ActivityInclusionRequest(true));

        // then
        assertThat(response.getIsIncluded()).isTrue();
    }

    @Test
    void updateInclusion_실패_존재하지_않는_활동() {
        // given
        given(activityHistoryRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> activitiesService.updateInclusion(1L, 99L, new ActivityInclusionRequest(false)))
                .isInstanceOf(ActivityNotFoundException.class);
    }

    @Test
    void updateInclusion_실패_타인_활동_이력() {
        // given
        ActivityHistory otherActivity = ActivityHistory.builder()
                .activityHistoryId(10L)
                .user(otherUser)
                .activityType(ActivityType.GITHUB_COMMIT)
                .title("타인 커밋")
                .activityDate(LocalDateTime.now())
                .build();

        given(activityHistoryRepository.findById(10L)).willReturn(Optional.of(otherActivity));

        // when & then
        assertThatThrownBy(() -> activitiesService.updateInclusion(1L, 10L, new ActivityInclusionRequest(false)))
                .isInstanceOf(ActivityAccessDeniedException.class);
    }

    // ===== getGrowthReport =====

    private TechStack buildTechStack(Long id, String name) {
        return TechStack.builder()
                .techStackId(id)
                .techName(name)
                .iconUrl("https://icon.example/" + name + ".png")
                .color("#000000")
                .build();
    }

    private void stubGrowthReportRepositories(
            List<Object[]> topTechRaws,
            long totalCount,
            List<Object[]> recentGrowthRaws,
            List<LocalDateTime> activityDates,
            List<UserTechStack> topScoreStacks,
            List<Object[]> monthlyRaws
    ) {
        // findTechStackCountsByUserId는 top(10개)과 ranking(5개) 두 번 호출됨
        given(activityHistoryTechStackRepository.findTechStackCountsByUserId(eq(1L), any()))
                .willReturn(topTechRaws);
        // findTechStackCountsSince는 최근 30일 성장 기술 조회
        given(activityHistoryTechStackRepository.findTechStackCountsSince(eq(1L), any(), any()))
                .willReturn(recentGrowthRaws);
        given(activityHistoryRepository.countByUserUserId(1L)).willReturn(totalCount);
        given(activityHistoryRepository.findActivityDatesByUserId(1L)).willReturn(activityDates);
        given(userTechStackRepository.findTop6ByUserUserIdOrderByScoreDesc(1L)).willReturn(topScoreStacks);
        given(activityHistoryRepository.findMonthlyActivityCountsByUserId(1L)).willReturn(monthlyRaws);
    }

    @Test
    void getGrowthReport_성공_정상_데이터() {
        // given
        TechStack springStack = buildTechStack(1L, "Spring Boot");
        TechStack javaStack = buildTechStack(2L, "Java");

        List<Object[]> topTechRaws = new java.util.ArrayList<>();
        topTechRaws.add(new Object[]{springStack, 10L});
        topTechRaws.add(new Object[]{javaStack, 5L});
        List<Object[]> recentGrowthRaws = new java.util.ArrayList<>();
        recentGrowthRaws.add(new Object[]{springStack, 3L});

        List<LocalDateTime> activityDates = List.of(
                LocalDateTime.of(2025, 3, 1, 12, 0),
                LocalDateTime.of(2025, 3, 2, 12, 0),
                LocalDateTime.of(2025, 3, 3, 12, 0)
        );

        UserTechStack uts = UserTechStack.builder()
                .userTechStackId(1L)
                .techStack(springStack)
                .score(80.0)
                .build();

        List<Object[]> monthlyRaws = new java.util.ArrayList<>();
        monthlyRaws.add(new Object[]{2025, 3, ActivityType.GITHUB_COMMIT, 5L});

        stubGrowthReportRepositories(topTechRaws, 15L, recentGrowthRaws, activityDates, List.of(uts), monthlyRaws);

        // when
        GrowthReportResponse response = activitiesService.getGrowthReport(1L);

        // then
        assertThat(response.getTopTechStacks()).hasSize(2);
        assertThat(response.getTopTechStacks().get(0).getTechName()).isEqualTo("Spring Boot");
        assertThat(response.getTotalActivityCount()).isEqualTo(15L);
        assertThat(response.getRecentGrowthTech().getTechName()).isEqualTo("Spring Boot");
        assertThat(response.getMaxStreakDays()).isEqualTo(3);
        assertThat(response.getTechScoreSnapshot()).hasSize(1);
        assertThat(response.getTechScoreSnapshot().get(0).getScore()).isEqualTo(80.0);
        assertThat(response.getMonthlyActivityCounts()).hasSize(1);
        assertThat(response.getTechActivityRanking()).hasSize(2);
    }

    @Test
    void getGrowthReport_성공_활동_없으면_빈_응답() {
        // given
        stubGrowthReportRepositories(List.of(), 0L, List.of(), List.of(), List.of(), List.of());

        // when
        GrowthReportResponse response = activitiesService.getGrowthReport(1L);

        // then
        assertThat(response.getTopTechStacks()).isEmpty();
        assertThat(response.getTotalActivityCount()).isEqualTo(0L);
        assertThat(response.getRecentGrowthTech()).isNull();
        assertThat(response.getMaxStreakDays()).isEqualTo(0);
        assertThat(response.getTechScoreSnapshot()).isEmpty();
        assertThat(response.getMonthlyActivityCounts()).isEmpty();
        assertThat(response.getTechActivityRanking()).isEmpty();
    }

    @Test
    void getGrowthReport_스트릭_계산_연속되지_않은_날짜() {
        // given — 3/1, 3/3, 3/5: 연속 없음 → maxStreak = 1
        List<LocalDateTime> activityDates = List.of(
                LocalDateTime.of(2025, 3, 1, 12, 0),
                LocalDateTime.of(2025, 3, 3, 12, 0),
                LocalDateTime.of(2025, 3, 5, 12, 0)
        );
        stubGrowthReportRepositories(List.of(), 3L, List.of(), activityDates, List.of(), List.of());

        // when
        GrowthReportResponse response = activitiesService.getGrowthReport(1L);

        // then
        assertThat(response.getMaxStreakDays()).isEqualTo(1);
    }

    @Test
    void getGrowthReport_스트릭_계산_같은_날_여러_활동은_1일로_카운트() {
        // given — 3/1 두 번, 3/2 한 번 → 연속 2일
        List<LocalDateTime> activityDates = List.of(
                LocalDateTime.of(2025, 3, 1, 9, 0),
                LocalDateTime.of(2025, 3, 1, 18, 0),
                LocalDateTime.of(2025, 3, 2, 12, 0)
        );
        stubGrowthReportRepositories(List.of(), 3L, List.of(), activityDates, List.of(), List.of());

        // when
        GrowthReportResponse response = activitiesService.getGrowthReport(1L);

        // then
        assertThat(response.getMaxStreakDays()).isEqualTo(2);
    }
}
