package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;

public interface AuthService {

    AuthTokenBundle loginWithGoogle(String code);

    AuthReissueTokenBundle reissueAccessToken(String refreshToken);

    void logout(String authorizationHeader);

    GithubAuthTokenBundle linkGithub(String code, String state);
}
