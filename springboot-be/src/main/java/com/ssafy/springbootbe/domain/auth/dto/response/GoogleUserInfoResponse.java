package com.ssafy.springbootbe.domain.auth.dto.response;

import tools.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleUserInfoResponse {

    private String sub;
    private String email;

    @JsonProperty("picture")
    private String picture;
}
