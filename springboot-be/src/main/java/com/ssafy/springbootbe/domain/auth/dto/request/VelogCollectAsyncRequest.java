package com.ssafy.springbootbe.domain.auth.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelogCollectAsyncRequest {

    private Long userId;
    private String velogUsername;
}
