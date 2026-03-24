package com.ssafy.springbootbe.domain.curricula.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class SkillStatDto {

    private String skill;
    private Integer count;
}