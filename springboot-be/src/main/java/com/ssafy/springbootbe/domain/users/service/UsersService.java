package com.ssafy.springbootbe.domain.users.service;

import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;

import java.util.Map;

public interface UsersService {

    UserProfileResponse findProfile(Long userId);

    UserProfileUpdateResponse updateProfile(Long userId, UserProfileUpdateRequest request);

    Map<String, Boolean> updateDarkMode(Long userId, DarkModeUpdateRequest request);

    void deleteUser(Long userId, String token);
}
