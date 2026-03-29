package com.ssafy.springbootbe.domain.activities.service;

import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistory;
import com.ssafy.springbootbe.persistence.activity.entity.ActivityHistoryTechStack;
import com.ssafy.springbootbe.persistence.activity.repository.ActivityHistoryTechStackRepository;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.entity.UserTechStack;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import com.ssafy.springbootbe.persistence.user.repository.UserTechStackRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LearningStateServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private UserTechStackRepository userTechStackRepository;
    @Mock private ActivityHistoryTechStackRepository activityHistoryTechStackRepository;

    @InjectMocks
    private LearningStateServiceImpl learningStateService;

    @Test
    void recalculateForUser_활동가중치와시간감쇠를반영해점수를업데이트한다() {
        User user = User.builder().userId(1L).build();
        TechStack java = TechStack.builder().techStackId(10L).techName("Java").build();

        ActivityHistory recentVelog = ActivityHistory.builder()
                .activityHistoryId(100L)
                .user(user)
                .activityType(ActivityType.VELOG_POST)
                .activityDate(LocalDateTime.now().minusMonths(2))
                .title("velog")
                .build();
        ActivityHistory oldCommit = ActivityHistory.builder()
                .activityHistoryId(101L)
                .user(user)
                .activityType(ActivityType.GITHUB_COMMIT)
                .activityDate(LocalDateTime.now().minusMonths(6))
                .title("commit")
                .build();

        ActivityHistoryTechStack velogLink = ActivityHistoryTechStack.builder()
                .activityHistory(recentVelog)
                .techStack(java)
                .build();
        ActivityHistoryTechStack commitLink = ActivityHistoryTechStack.builder()
                .activityHistory(oldCommit)
                .techStack(java)
                .build();

        UserTechStack existing = UserTechStack.builder()
                .userTechStackId(1L)
                .user(user)
                .techStack(java)
                .score(0.0)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(activityHistoryTechStackRepository.findIncludedByUserId(1L)).willReturn(List.of(velogLink, commitLink));
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of(existing));

        learningStateService.recalculateForUser(1L);

        assertThat(existing.getScore()).isEqualTo(1.08);
        verify(userTechStackRepository, never()).save(any(UserTechStack.class));
    }

    @Test
    void recalculateForUser_기존에없던기술은새로운유저기술스택으로저장한다() {
        User user = User.builder().userId(1L).build();
        TechStack spring = TechStack.builder().techStackId(20L).techName("Spring").build();

        ActivityHistory quiz = ActivityHistory.builder()
                .activityHistoryId(200L)
                .user(user)
                .activityType(ActivityType.QUIZ)
                .activityDate(LocalDateTime.now().minusMonths(1))
                .title("quiz")
                .build();

        ActivityHistoryTechStack quizLink = ActivityHistoryTechStack.builder()
                .activityHistory(quiz)
                .techStack(spring)
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(activityHistoryTechStackRepository.findIncludedByUserId(1L)).willReturn(List.of(quizLink));
        given(userTechStackRepository.findByUserUserId(1L)).willReturn(List.of());

        learningStateService.recalculateForUser(1L);

        verify(userTechStackRepository).save(any(UserTechStack.class));
    }
}
