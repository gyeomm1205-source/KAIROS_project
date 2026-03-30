package com.ssafy.springbootbe.domain.activities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivitySyncResponse {

    private String provider;
    private LocalDateTime syncedAt;
    private int newCount;
}
