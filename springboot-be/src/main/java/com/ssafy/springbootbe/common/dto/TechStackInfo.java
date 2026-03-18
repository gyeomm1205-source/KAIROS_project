package com.ssafy.springbootbe.common.dto;

import com.ssafy.springbootbe.persistence.techstack.entity.TechStack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TechStackInfo {

    private Long techStackId;
    private String techName;
    private String iconUrl;
    private String color;

    public static TechStackInfo from(TechStack techStack) {
        return TechStackInfo.builder()
                .techStackId(techStack.getTechStackId())
                .techName(techStack.getTechName())
                .iconUrl(techStack.getIconUrl())
                .color(techStack.getColor())
                .build();
    }
}