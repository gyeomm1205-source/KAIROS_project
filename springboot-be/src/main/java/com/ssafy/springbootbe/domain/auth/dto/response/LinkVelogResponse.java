package com.ssafy.springbootbe.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkVelogResponse {

    private String velogUsername;
    private String velogTaskId;

    public static LinkVelogResponse of(String velogUsername, String velogTaskId) {
        return LinkVelogResponse.builder()
                .velogUsername(velogUsername)
                .velogTaskId(velogTaskId)
                .build();
    }
}
