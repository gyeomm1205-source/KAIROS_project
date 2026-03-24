package com.ssafy.springbootbe.domain.curricula.dto.response;

import com.ssafy.springbootbe.persistence.curriculum.type.SyncStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarSyncDto {

    private SyncStatus syncStatus;
    private String googleEventId;
}