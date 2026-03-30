package com.ssafy.springbootbe.domain.activities.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.activities.dto.request.ActivityInclusionRequest;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityHistoryResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityInclusionResponse;
import com.ssafy.springbootbe.domain.activities.dto.response.ActivityPageResponse;
import com.ssafy.springbootbe.domain.activities.exception.ActivityAccessDeniedException;
import com.ssafy.springbootbe.domain.activities.exception.ActivityNotFoundException;
import com.ssafy.springbootbe.domain.activities.service.ActivitiesService;
import com.ssafy.springbootbe.domain.activities.service.ActivitySyncService;
import com.ssafy.springbootbe.persistence.activity.type.ActivityType;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ActivitiesControllerTest {

    @Mock private ActivitiesService activitiesService;
    @Mock private ActivitySyncService activitySyncService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new ActivitiesController(activitiesService, activitySyncService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    // ===== GET /activities =====

    @Test
    void 활동_이력_조회_성공() throws Exception {
        // given
        ActivityHistoryResponse item = ActivityHistoryResponse.builder()
                .activityHistoryId(10L)
                .activityType(ActivityType.GITHUB_COMMIT)
                .title("커밋 제목")
                .isIncluded(true)
                .activityDate(LocalDateTime.of(2025, 3, 1, 12, 0))
                .techStacks(List.of())
                .build();

        ActivityPageResponse response = ActivityPageResponse.builder()
                .total(1L)
                .page(1)
                .size(20)
                .items(List.of(item))
                .build();

        given(activitiesService.findActivities(eq(USER_ID), any(), any(), anyString(), anyInt(), anyInt()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/activities")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].activityHistoryId").value(10))
                .andExpect(jsonPath("$.items[0].title").value("커밋 제목"))
                .andExpect(jsonPath("$.items[0].isIncluded").value(true));
    }

    @Test
    void 활동_이력_조회_연도_월_필터() throws Exception {
        // given
        ActivityPageResponse response = ActivityPageResponse.builder()
                .total(0L).page(1).size(20).items(List.of()).build();

        given(activitiesService.findActivities(eq(USER_ID), eq(2025), eq(3), anyString(), anyInt(), anyInt()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/activities")
                        .principal(authenticatedUser())
                        .param("year", "2025")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void 활동_이력_조회_오래된순_정렬() throws Exception {
        // given
        ActivityPageResponse response = ActivityPageResponse.builder()
                .total(0L).page(1).size(20).items(List.of()).build();

        given(activitiesService.findActivities(eq(USER_ID), any(), any(), eq("oldest"), anyInt(), anyInt()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/activities")
                        .principal(authenticatedUser())
                        .param("sort", "oldest"))
                .andExpect(status().isOk());
    }

    @Test
    void 활동_이력_조회_빈_결과() throws Exception {
        // given
        ActivityPageResponse response = ActivityPageResponse.builder()
                .total(0L).page(1).size(20).items(List.of()).build();

        given(activitiesService.findActivities(any(), any(), any(), anyString(), anyInt(), anyInt()))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/activities")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty());
    }

    // ===== PATCH /activities/{id}/inclusion =====

    @Test
    void 활동_이력_포함여부_수정_성공_포함_해제() throws Exception {
        // given
        ActivityInclusionResponse response = ActivityInclusionResponse.builder()
                .activityHistoryId(10L)
                .isIncluded(false)
                .build();

        given(activitiesService.updateInclusion(eq(USER_ID), eq(10L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/activities/10/inclusion")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityInclusionRequest(false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activityHistoryId").value(10))
                .andExpect(jsonPath("$.isIncluded").value(false));
    }

    @Test
    void 활동_이력_포함여부_수정_성공_포함_복원() throws Exception {
        // given
        ActivityInclusionResponse response = ActivityInclusionResponse.builder()
                .activityHistoryId(10L)
                .isIncluded(true)
                .build();

        given(activitiesService.updateInclusion(eq(USER_ID), eq(10L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/activities/10/inclusion")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityInclusionRequest(true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isIncluded").value(true));
    }

    @Test
    void 활동_이력_포함여부_수정_실패_존재하지_않는_활동() throws Exception {
        // given
        given(activitiesService.updateInclusion(eq(USER_ID), eq(99L), any()))
                .willThrow(new ActivityNotFoundException(99L));

        // when & then
        mockMvc.perform(patch("/activities/99/inclusion")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityInclusionRequest(false))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 활동_이력_포함여부_수정_실패_타인_활동_이력() throws Exception {
        // given
        given(activitiesService.updateInclusion(eq(USER_ID), eq(10L), any()))
                .willThrow(new ActivityAccessDeniedException(10L));

        // when & then
        mockMvc.perform(patch("/activities/10/inclusion")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ActivityInclusionRequest(false))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        LoginUserPrincipal principal = LoginUserPrincipal.builder()
                .userId(USER_ID)
                .nickName("tester")
                .email("test@test.com")
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