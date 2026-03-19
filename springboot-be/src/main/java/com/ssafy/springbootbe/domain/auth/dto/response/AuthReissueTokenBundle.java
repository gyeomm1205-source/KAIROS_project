package com.ssafy.springbootbe.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthReissueTokenBundle {

    private AuthReissueResponse response;
    private String refreshToken;
}
