package com.ssafy.springbootbe.domain.onboarding.controller;

import com.ssafy.springbootbe.common.dto.DevPositionInfo;
import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingMetaResponse;
import com.ssafy.springbootbe.domain.onboarding.dto.request.OnboardingSurveyRequest;
import com.ssafy.springbootbe.domain.onboarding.dto.response.OnboardingSurveyResponse;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingAccessDeniedException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingMetaRetrievalException;
import com.ssafy.springbootbe.domain.onboarding.exception.OnboardingReferenceNotFoundException;
import com.ssafy.springbootbe.domain.onboarding.service.OnboardingService;
import com.ssafy.springbootbe.persistence.user.type.CurriculumCategory;
import com.ssafy.springbootbe.persistence.user.type.UserPosition;
import com.ssafy.springbootbe.persistence.user.type.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OnboardingControllerTest {

    @Mock
    private OnboardingService onboardingService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new OnboardingController(onboardingService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 온보딩_메타_조회_성공() throws Exception {
        // given
        OnboardingMetaResponse response = OnboardingMetaResponse.builder()
                .techStacks(List.of(
                        TechStackInfo.builder()
                                .techStackId(1L)
                                .techName("Java")
                                .iconUrl("https://cdn.example/java.png")
                                .color("#007396")
                                .build()
                ))
                .devPositions(List.of(
                        DevPositionInfo.builder()
                                .devPositionId(1L)
                                .positionName("Backend")
                                .build()
                ))
                .build();

        given(onboardingService.getSurveyMeta()).willReturn(response);

        // when & then
        mockMvc.perform(get("/onboarding/meta")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.techStacks[0].techStackId").value(1L))
                .andExpect(jsonPath("$.techStacks[0].techName").value("Java"))
                .andExpect(jsonPath("$.techStacks[0].iconUrl").value("https://cdn.example/java.png"))
                .andExpect(jsonPath("$.techStacks[0].color").value("#007396"))
                .andExpect(jsonPath("$.devPositions[0].devPositionId").value(1L))
                .andExpect(jsonPath("$.devPositions[0].positionName").value("Backend"));
    }

    @Test
    void 온보딩_메타_조회_실패_DB조회오류면_500() throws Exception {
        // given
        given(onboardingService.getSurveyMeta())
                .willThrow(new OnboardingMetaRetrievalException("온보딩 메타데이터 조회에 실패했습니다.", new RuntimeException()));

        // when & then
        mockMvc.perform(get("/onboarding/meta")
                        .principal(authenticatedUser()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("SERVER_ERROR"));
    }

    @Test
    void 설문_제출_성공() throws Exception {
        // given
        OnboardingSurveyRequest request = new OnboardingSurveyRequest(
                UserPosition.STUDENT,
                true,
                List.of(1L, 3L),
                List.of(2L, 5L, 7L),
                List.of(CurriculumCategory.THEORY, CurriculumCategory.PRACTICE)
        );
        OnboardingSurveyResponse response = OnboardingSurveyResponse.builder()
                .userId(1L)
                .status(UserStatus.SURVEYED)
                .considerPersonalSchedule(true)
                .build();

        given(onboardingService.submitSurvey(eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.status").value("SURVEYED"))
                .andExpect(jsonPath("$.considerPersonalSchedule").value(true))
                ;
    }

    @Test
    void 설문_제출_실패_GUEST가_아니면_403() throws Exception {
        // given
        given(onboardingService.submitSurvey(eq(1L), any()))
                .willThrow(new OnboardingAccessDeniedException(1L, UserStatus.SURVEYED));

        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    @Test
    void 설문_제출_실패_position_ENUM이_잘못되면_400() throws Exception {
        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": "INVALID",
                                  "desiredPositionIds": [1],
                                  "techStackIds": [2],
                                  "curriculumCategories": ["THEORY"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 설문_제출_실패_curriculumCategories_ENUM이_잘못되면_400() throws Exception {
        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "position": "STUDENT",
                                  "desiredPositionIds": [1],
                                  "techStackIds": [2],
                                  "curriculumCategories": ["INVALID"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 설문_제출_실패_존재하지_않는_techStackId면_404() throws Exception {
        // given
        given(onboardingService.submitSurvey(eq(1L), any()))
                .willThrow(new OnboardingReferenceNotFoundException("존재하지 않는 techStackId가 포함되어 있습니다."));

        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 설문_제출_실패_존재하지_않는_devPositionId면_404() throws Exception {
        // given
        given(onboardingService.submitSurvey(eq(1L), any()))
                .willThrow(new OnboardingReferenceNotFoundException("존재하지 않는 devPositionId가 포함되어 있습니다."));

        // when & then
        mockMvc.perform(post("/onboarding/survey")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
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

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        LoginUserPrincipal principal = LoginUserPrincipal.builder()
                .userId(1L)
                .nickName("guest")
                .email("guest@gmail.com")
                .build();
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }

    private String validRequestBody() {
        return """
                {
                  "position": "STUDENT",
                  "considerPersonalSchedule": true,
                  "desiredPositionIds": [1, 3],
                  "techStackIds": [2, 5, 7],
                  "curriculumCategories": ["THEORY", "PRACTICE"]
                }
                """;
    }
}
