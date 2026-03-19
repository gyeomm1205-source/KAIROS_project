package com.ssafy.springbootbe.domain.users.dto.response;

import com.ssafy.springbootbe.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long userId;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private UserPosition position;
    private Boolean darkModeEnabled;
    private Boolean considerPersonalSchedule;
    private List<DevPositionInfo> desiredPositions;
    private List<TechStackInfo> techStacks;
    private List<CurriculumCategory> curriculumCategories;

    public static UserProfileResponse of(User user,
                                         List<DevPositionInfo> desiredPositions,
                                         List<TechStackInfo> techStacks,
                                         List<CurriculumCategory> curriculumCategories) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .position(user.getPosition())
                .darkModeEnabled(user.getDarkModeEnabled())
                .considerPersonalSchedule(user.getCalendarSyncEnabled())
                .desiredPositions(desiredPositions)
                .techStacks(techStacks)
                .curriculumCategories(curriculumCategories)
                .build();
    }
}
