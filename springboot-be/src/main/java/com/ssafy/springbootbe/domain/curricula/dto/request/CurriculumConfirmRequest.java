package com.ssafy.springbootbe.domain.curricula.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CurriculumConfirmRequest {

    @NotBlank(message = "curriculumPreviewKey는 필수입니다.")
    private String curriculumPreviewKey;
}
