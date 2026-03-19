package com.ssafy.springbootbe.domain.users.dto.request;

import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileUpdateRequest {

    private UserPosition position;
    private List<Long> desiredPositionIds;
    private List<Long> techStackIds;
    private List<CurriculumCategory> curriculumCategories;
    private Boolean considerPersonalSchedule;
}
