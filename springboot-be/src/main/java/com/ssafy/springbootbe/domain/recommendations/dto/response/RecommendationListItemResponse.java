package com.ssafy.springbootbe.domain.recommendations.dto.response;

import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationListItemResponse {

    private Long curriculumId;
    private CurriculumStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<TechStackInfo> techStacks;
    private Boolean hasRecommendation;
}
