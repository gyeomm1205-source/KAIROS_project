package com.ssafy.springbootbe.domain.users.dto.response;

import com.ssafy.springbootbe.domain.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.domain.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserProfileUpdateResponse {

    private UserPosition position;
    private List<DevPositionInfo> desiredPositions;
    private List<TechStackInfo> techStacks;
    private List<CurriculumCategory> curriculumCategories;
    private Boolean considerPersonalSchedule;
}
