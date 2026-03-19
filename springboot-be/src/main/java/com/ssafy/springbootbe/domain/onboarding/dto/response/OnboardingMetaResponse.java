package com.ssafy.springbootbe.domain.onboarding.dto.response;

import com.ssafy.springbootbe.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingMetaResponse {

    private List<TechStackInfo> techStacks;
    private List<DevPositionInfo> devPositions;
}
