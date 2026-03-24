package com.ssafy.springbootbe.domain.curricula.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechDetailRequest {

    private String techName;
    private Integer proficiencyPercentage;
    private Integer usageCount;
}
