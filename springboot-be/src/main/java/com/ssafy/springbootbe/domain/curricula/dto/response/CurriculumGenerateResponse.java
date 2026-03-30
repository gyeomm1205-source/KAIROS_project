package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CurriculumGenerateResponse {

    private PreviewReasonDto recommendationReason;
    private List<String> techStacks;
    private List<PreviewNodeDto> nodes;
    private PreviewOptionDto optionA;
    private PreviewOptionDto optionB;
}
