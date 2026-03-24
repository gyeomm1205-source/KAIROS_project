package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PreviewNodeDto {

    private String title;
    private String description;
    private LocalDate scheduledDate;
    private Integer expectedMinutes;
}