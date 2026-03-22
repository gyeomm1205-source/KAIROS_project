package com.ssafy.springbootbe.domain.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import com.ssafy.springbootbe.domain.users.service.UsersService;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UsersService usersService;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private RedisService redisService;

    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        given(claims.get("userId", Long.class)).willReturn(USER_ID);
        given(jwtUtils.getClaims("test-token")).willReturn(claims);
    }

    // ===== GET /users/me =====

    @Test
    void 프로필_조회_성공() throws Exception {
        // given
        UserProfileResponse response = UserProfileResponse.builder()
                .userId(1L)
                .email("user@gmail.com")
                .nickname("teddynu")
                .darkModeEnabled(false)
                .desiredPositions(List.of())
                .techStacks(List.of())
                .curriculumCategories(List.of())
                .build();

        given(usersService.findProfile(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/me")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("user@gmail.com"))
                .andExpect(jsonPath("$.nickname").value("teddynu"))
                .andExpect(jsonPath("$.darkModeEnabled").value(false));
    }

    @Test
    void 프로필_조회_실패_유저_없음() throws Exception {
        // given
        given(usersService.findProfile(USER_ID)).willThrow(new UserNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(get("/users/me")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== PATCH /users/me/profile =====

    @Test
    void 프로필_수정_성공() throws Exception {
        // given
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                UserPosition.JUNIOR, null, null, null, null);

        UserProfileUpdateResponse response = UserProfileUpdateResponse.builder()
                .position(UserPosition.JUNIOR)
                .desiredPositions(List.of())
                .techStacks(List.of())
                .curriculumCategories(List.of())
                .considerPersonalSchedule(false)
                .build();

        given(usersService.updateProfile(eq(USER_ID), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/users/me/profile")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.position").value("JUNIOR"));
    }

    @Test
    void 프로필_수정_실패_유저_없음() throws Exception {
        // given
        given(usersService.updateProfile(eq(USER_ID), any()))
                .willThrow(new UserNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(patch("/users/me/profile")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserProfileUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== PATCH /users/me/settings/dark-mode =====

    @Test
    void 다크모드_수정_성공_활성화() throws Exception {
        // given
        given(usersService.updateDarkMode(eq(USER_ID), any()))
                .willReturn(Map.of("darkModeEnabled", true));

        // when & then
        mockMvc.perform(patch("/users/me/settings/dark-mode")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarkModeUpdateRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkModeEnabled").value(true));
    }

    @Test
    void 다크모드_수정_성공_비활성화() throws Exception {
        // given
        given(usersService.updateDarkMode(eq(USER_ID), any()))
                .willReturn(Map.of("darkModeEnabled", false));

        // when & then
        mockMvc.perform(patch("/users/me/settings/dark-mode")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarkModeUpdateRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.darkModeEnabled").value(false));
    }

    @Test
    void 다크모드_수정_실패_유저_없음() throws Exception {
        // given
        given(usersService.updateDarkMode(eq(USER_ID), any()))
                .willThrow(new UserNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(patch("/users/me/settings/dark-mode")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DarkModeUpdateRequest(true))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== DELETE /users/me =====

    @Test
    void 회원_탈퇴_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void 회원_탈퇴_실패_유저_없음() throws Exception {
        // given
        doThrow(new UserNotFoundException(USER_ID))
                .when(usersService).deleteUser(eq(USER_ID), any());

        // when & then
        mockMvc.perform(delete("/users/me")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
