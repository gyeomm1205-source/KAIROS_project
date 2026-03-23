package com.ssafy.springbootbe.domain.recommendations.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListItemResponse;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationListResponse;
import com.ssafy.springbootbe.domain.recommendations.service.RecommendationsService;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RecommendationControllerTest {

    @Mock
    private RecommendationsService recommendationsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RecommendationController(recommendationsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 추천_목록_조회_응답_필드명이_camelCase와_일치한다() throws Exception {
        // given
        RecommendationListResponse response = RecommendationListResponse.builder()
                .items(List.of(
                        RecommendationListItemResponse.builder()
                                .curriculumId(10L)
                                .status(CurriculumStatus.ACTIVE)
                                .startDate(LocalDate.of(2025, 1, 2))
                                .endDate(LocalDate.of(2025, 2, 1))
                                .techStacks(List.of(
                                        TechStackInfo.builder()
                                                .techStackId(1L)
                                                .techName("Java")
                                                .iconUrl("https://example.com/java.png")
                                                .color("#007396")
                                                .build()
                                ))
                                .hasRecommendation(true)
                                .build()
                ))
                .build();
        given(recommendationsService.findRecommendations(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/recommendations").principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].curriculumId").value(10))
                .andExpect(jsonPath("$.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.items[0].startDate").value("2025-01-02"))
                .andExpect(jsonPath("$.items[0].endDate").value("2025-02-01"))
                .andExpect(jsonPath("$.items[0].techStacks[0].techStackId").value(1))
                .andExpect(jsonPath("$.items[0].techStacks[0].techName").value("Java"))
                .andExpect(jsonPath("$.items[0].techStacks[0].iconUrl").value("https://example.com/java.png"))
                .andExpect(jsonPath("$.items[0].techStacks[0].color").value("#007396"))
                .andExpect(jsonPath("$.items[0].hasRecommendation").value(true));
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        LoginUserPrincipal principal = LoginUserPrincipal.builder()
                .userId(1L)
                .nickName("tester")
                .email("tester@example.com")
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
