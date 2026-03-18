package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;
import com.ssafy.springbootbe.domain.auth.dto.response.GithubAuthTokenBundle;

import java.net.URI;

public interface AuthService {

    AuthTokenBundle handleGoogleCallback(String code);

    URI buildGithubAuthorizationRedirect(String authorizationHeader);

    GithubAuthTokenBundle handleGithubCallback(String code, String state);
}
