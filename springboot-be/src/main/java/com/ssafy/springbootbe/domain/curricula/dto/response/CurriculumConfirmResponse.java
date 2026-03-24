package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumConfirmResponse {

    private Long curriculumId;
    private CurriculumStatus status;
    private LocalDateTime createdAt;
    private List<ConfirmNodeDto> nodes;
}