package com.ssafy.springbootbe.domain.analysis.service;

import com.ssafy.springbootbe.domain.analysis.dto.request.AnalysisCompleteRequest;
import com.ssafy.springbootbe.domain.analysis.dto.response.AnalysisCompleteResponse;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryRepository;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.techstack.repository.TechStackRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private TechStackRepository techStackRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private ActivityHistoryRepository activityHistoryRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;

    @InjectMocks
    private AnalysisServiceImpl analysisService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .userId(1L)
                .email("user@test.com")
                .nickname("tester")
                .build();
    }

    private AnalysisCompleteRequest.ActivityItem buildActivityItem(String activityType, String summary,
                                                                    List<String> techStacks) {
        AnalysisCompleteRequest.ActivityItem item = new AnalysisCompleteRequest.ActivityItem();
        ReflectionTestUtils.setField(item, "activityType", activityType);
        ReflectionTestUtils.setField(item, "summary", summary);
        ReflectionTestUtils.setField(item, "activityDate", LocalDateTime.of(2025, 3, 10, 12, 0));
        ReflectionTestUtils.setField(item, "techStacks", techStacks);
        return item;
    }

    private AnalysisCompleteRequest buildRequest(Long userId,
                                                  List<AnalysisCompleteRequest.ActivityItem> github,
                                                  List<AnalysisCompleteRequest.ActivityItem> velog) {
        AnalysisCompleteRequest request = new AnalysisCompleteRequest();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "taskId", "task-001");
        ReflectionTestUtils.setField(request, "githubActivities", github);
        ReflectionTestUtils.setField(request, "velogActivities", velog);
        return request;
    }

    // ===== complete =====

    @Test
    void complete_성공_github와_velog_활동_저장() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        List<AnalysisCompleteRequest.ActivityItem> githubItems = List.of(
                buildActivityItem("GITHUB_COMMIT", "Spring Boot 커밋", List.of())
        );
        List<AnalysisCompleteRequest.ActivityItem> velogItems = List.of(
                buildActivityItem("VELOG_POST", "Velog 포스트 작성", List.of())
        );

        AnalysisCompleteRequest request = buildRequest(1L, githubItems, velogItems);

        // when
        AnalysisCompleteResponse response = analysisService.complete(request);

        // then
        assertThat(response.getMessage()).isEqualTo("activity_history saved.");
        assertThat(response.getSavedCount()).isEqualTo(2);
        verify(activityHistoryRepository, times(2)).save(any(ActivityHistory.class));
    }

    @Test
    void complete_성공_activities가_null이면_0_반환() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        AnalysisCompleteRequest request = buildRequest(1L, null, null);

        // when
        AnalysisCompleteResponse response = analysisService.complete(request);

        // then
        assertThat(response.getSavedCount()).isEqualTo(0);
        verify(activityHistoryRepository, never()).save(any());
    }

    @Test
    void complete_존재하지_않는_userId면_IllegalArgumentException() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());
        AnalysisCompleteRequest request = buildRequest(99L, List.of(), List.of());

        // when & then
        assertThatThrownBy(() -> analysisService.complete(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("userId=99");
    }

    @Test
    void complete_지원하지_않는_activityType_무시() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        List<AnalysisCompleteRequest.ActivityItem> items = List.of(
                buildActivityItem("GITHUB_COMMIT", "유효한 커밋", List.of()),
                buildActivityItem("UNKNOWN_TYPE", "지원하지 않는 타입", List.of())
        );
        AnalysisCompleteRequest request = buildRequest(1L, items, List.of());

        // when
        AnalysisCompleteResponse response = analysisService.complete(request);

        // then
        assertThat(response.getSavedCount()).isEqualTo(1); // UNKNOWN_TYPE은 무시됨
    }

    @Test
    void complete_techStack_DB에_없으면_연결_생략하고_savedCount_정상() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(techStackRepository.findByTechName("UnknownStack")).willReturn(Optional.empty());

        List<AnalysisCompleteRequest.ActivityItem> items = List.of(
                buildActivityItem("GITHUB_COMMIT", "커밋 요약", List.of("UnknownStack"))
        );
        AnalysisCompleteRequest request = buildRequest(1L, items, List.of());

        // when
        AnalysisCompleteResponse response = analysisService.complete(request);

        // then
        assertThat(response.getSavedCount()).isEqualTo(1); // 저장은 되고 techStack 연결만 생략
        verify(activityHistoryTechStackRepository).saveAll(List.of());
    }

    @Test
    void complete_techStack_DB에_있으면_연결_저장() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        TechStack techStack = TechStack.builder()
                .techStackId(1L)
                .techName("Spring Boot")
                .build();
        given(techStackRepository.findByTechName("Spring Boot")).willReturn(Optional.of(techStack));

        List<AnalysisCompleteRequest.ActivityItem> items = List.of(
                buildActivityItem("GITHUB_COMMIT", "Spring Boot 커밋", List.of("Spring Boot"))
        );
        AnalysisCompleteRequest request = buildRequest(1L, items, List.of());

        // when
        AnalysisCompleteResponse response = analysisService.complete(request);

        // then
        assertThat(response.getSavedCount()).isEqualTo(1);
        verify(activityHistoryTechStackRepository).saveAll(any());
    }
}
