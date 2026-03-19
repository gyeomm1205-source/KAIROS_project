package com.ssafy.springbootbe.domain.activities.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ActivityPageResponse {

    private long total;
    private int page;
    private int size;
    private List<ActivityHistoryResponse> items;
}
