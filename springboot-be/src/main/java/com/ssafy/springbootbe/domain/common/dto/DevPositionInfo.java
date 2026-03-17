package com.ssafy.springbootbe.domain.common.dto;

import com.ssafy.springbootbe.persistence.position.entity.DevPosition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DevPositionInfo {

    private Long devPositionId;
    private String positionName;

    public static DevPositionInfo from(DevPosition devPosition) {
        return DevPositionInfo.builder()
                .devPositionId(devPosition.getDevPositionId())
                .positionName(devPosition.getPositionName())
                .build();
    }
}
