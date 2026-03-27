package com.ssafy.springbootbe.domain.curricula.dto.request;

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
public class RecentActivityDto {

    private String activityType;
    private String category;
    private String title;
    private String description;
    private LocalDateTime activityDate;
    private List<String> techStacks;
}
