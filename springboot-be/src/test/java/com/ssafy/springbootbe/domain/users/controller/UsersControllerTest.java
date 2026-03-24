package com.ssafy.springbootbe.domain.users.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.users.dto.request.DarkModeUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.request.UserProfileUpdateRequest;
import com.ssafy.springbootbe.domain.users.dto.response.ExternalAccountsResponse;
import com.ssafy.springbootbe.domain.users.dto.response.GithubExternalAccountResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileResponse;
import com.ssafy.springbootbe.domain.users.dto.response.UserProfileUpdateResponse;
import com.ssafy.springbootbe.domain.users.dto.response.VelogExternalAccountResponse;
import com.ssafy.springbootbe.domain.users.exception.GithubExternalAccountNotFoundException;
import com.ssafy.springbootbe.domain.users.exception.UserNotFoundException;
import com.ssafy.springbootbe.domain.users.service.UsersService;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UsersService usersService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new UsersController(usersService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
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
                        .principal(authenticatedUser()))
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
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== PATCH /users/me/profile =====

    @Test
    void 외부계정_조회_성공() throws Exception {
        // given
        ExternalAccountsResponse response = ExternalAccountsResponse.builder()
                .github(GithubExternalAccountResponse.builder()
                        .username("junghyun-dev")
                        .lastSyncedAt(LocalDateTime.of(2026, 3, 9, 14, 23))
                        .recentCommits(24)
                        .recentPrs(6)
                        .totalRepos(3)
                        .build())
                .velog(VelogExternalAccountResponse.builder()
                        .username("junghyun")
                        .lastSyncedAt(LocalDateTime.of(2026, 3, 9, 12, 10))
                        .totalPosts(8)
                        .latestPostDate(LocalDate.of(2026, 3, 7))
                        .totalViews(1247L)
                        .build())
                .build();

        given(usersService.findExternalAccounts(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(get("/users/me/external-accounts")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.github.username").value("junghyun-dev"))
                .andExpect(jsonPath("$.github.lastSyncedAt").value("2026-03-09T14:23:00"))
                .andExpect(jsonPath("$.github.recentCommits").value(24))
                .andExpect(jsonPath("$.github.recentPrs").value(6))
                .andExpect(jsonPath("$.github.totalRepos").value(3))
                .andExpect(jsonPath("$.velog.username").value("junghyun"))
                .andExpect(jsonPath("$.velog.lastSyncedAt").value("2026-03-09T12:10:00"))
                .andExpect(jsonPath("$.velog.totalPosts").value(8))
                .andExpect(jsonPath("$.velog.latestPostDate").value("2026-03-07"))
                .andExpect(jsonPath("$.velog.totalViews").value(1247));
    }

    @Test
    void 외부계정_조회_실패_깃허브_연동없음() throws Exception {
        // given
        given(usersService.findExternalAccounts(USER_ID))
                .willThrow(new GithubExternalAccountNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(get("/users/me/external-accounts")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

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
                        .principal(authenticatedUser())
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
                        .principal(authenticatedUser())
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
                        .principal(authenticatedUser())
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
                        .principal(authenticatedUser())
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
                        .principal(authenticatedUser())
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
                        .principal(authenticatedUser())
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    void 회원_탈퇴_실패_유저_없음() throws Exception {
        // given
        doThrow(new UserNotFoundException(USER_ID))
                .when(usersService).deleteUser(eq(USER_ID), any());

        // when & then
        mockMvc.perform(delete("/users/me")
                        .principal(authenticatedUser())
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        LoginUserPrincipal principal = LoginUserPrincipal.builder()
                .userId(USER_ID)
                .nickName("teddynu")
                .email("user@gmail.com")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private static class LoginUserPrincipalArgumentResolver implements HandlerMethodArgumentResolver {
        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                    && parameter.getParameterType().equals(LoginUserPrincipal.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                WebDataBinderFactory binderFactory
        ) {
            Authentication authentication = (Authentication) webRequest.getUserPrincipal();
            return authentication == null ? null : authentication.getPrincipal();
        }
    }
}
