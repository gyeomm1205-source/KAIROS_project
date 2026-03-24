package com.ssafy.springbootbe.domain.curricula.dto.request;

import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CurriculumNodeUpdateRequest {

    private String title;
    private String description;
    private LocalDate scheduledDate;
    private ProgressStatus progressStatus;

    public boolean hasNoFields() {
        return title == null && description == null && scheduledDate == null && progressStatus == null;
    }
}