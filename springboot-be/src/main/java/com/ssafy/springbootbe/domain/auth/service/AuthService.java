package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;

import java.net.URI;

public interface AuthService {

    AuthTokenBundle handleGoogleCallback(String code);

    URI buildGithubAuthorizationRedirect(String authorizationHeader);
}
