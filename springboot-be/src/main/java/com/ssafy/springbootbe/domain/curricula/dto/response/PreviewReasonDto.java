package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreviewReasonDto {

    private String summaryLine;
    private String userContext;
    private String aiInterpretation;
    private String curriculumRationale;
}