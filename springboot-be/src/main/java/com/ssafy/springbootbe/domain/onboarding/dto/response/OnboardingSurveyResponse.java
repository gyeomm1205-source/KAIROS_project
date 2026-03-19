package com.ssafy.springbootbe.domain.onboarding.dto.response;

import com.ssafy.springbootbe.persistence.analysis.type.AnalysisStatus;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingSurveyResponse {

    private Long userId;
    private UserStatus status;
    private Boolean considerPersonalSchedule;
    private Long analysisReportId;
    private AnalysisStatus analysisStatus;
}
