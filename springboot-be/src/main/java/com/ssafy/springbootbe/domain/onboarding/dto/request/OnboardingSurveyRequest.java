package com.ssafy.springbootbe.domain.onboarding.dto.request;

import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSurveyRequest {

    @NotNull(message = "position은 필수입니다.")
    private UserPosition position;

    @NotEmpty(message = "desiredPositionIds는 필수입니다.")
    private List<@NotNull(message = "desiredPositionIds에는 null이 포함될 수 없습니다.") Long> desiredPositionIds;

    @NotEmpty(message = "techStackIds는 필수입니다.")
    private List<@NotNull(message = "techStackIds에는 null이 포함될 수 없습니다.") Long> techStackIds;

    @NotEmpty(message = "curriculumCategories는 최소 1개 이상이어야 합니다.")
    private List<@NotNull(message = "curriculumCategories에는 null이 포함될 수 없습니다.")
            CurriculumCategory> curriculumCategories;
}
