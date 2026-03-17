package com.ssafy.springbootbe.domain.users.service;

import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;

public interface UsersService {

    UserProfileResponse findProfile(Long userId);

    UserProfileUpdateResponse updateProfile(Long userId, UserProfileUpdateRequest request);
}
