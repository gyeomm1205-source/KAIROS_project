package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.exception.ActivityAccessDeniedException;
import com.ssafy.springbootbe.domain.activities.exception.ActivityNotFoundException;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
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
}
