package com.ssafy.springbootbe.domain.auth.service;

import com.ssafy.springbootbe.domain.auth.dto.response.AuthTokenBundle;

public interface AuthService {

    AuthTokenBundle handleGoogleCallback(String code);
}
