package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.domain.auth.dto.request.LinkGithubRequest;
import com.ssafy.springbootbe.domain.auth.dto.request.LinkVelogRequest;
import com.ssafy.springbootbe.domain.auth.dto.response.LinkVelogResponse;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.AuthReissueTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;

public interface AuthService {

    AuthTokenBundle loginWithGoogle(String code);

    AuthReissueTokenBundle reissueAccessToken(String refreshToken);

    void logout(String authorizationHeader);

    GithubAuthTokenBundle linkGithub(String authorizationHeader, LinkGithubRequest request);

    LinkVelogResponse linkVelog(String authorizationHeader, LinkVelogRequest request);
}
