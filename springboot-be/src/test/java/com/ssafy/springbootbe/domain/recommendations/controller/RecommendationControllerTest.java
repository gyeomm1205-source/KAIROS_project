package com.ssafy.springbootbe.domain.recommendations.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.dto.TechStackInfo;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.recommendations.dto.response.RecommendationDetailResponse;
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

    @Test
    void 추천_상세_조회_응답_필드명이_camelCase와_일치하고_correctAnswer를_노출하지_않는다() throws Exception {
        // given
        RecommendationDetailResponse response = RecommendationDetailResponse.builder()
                .curriculumId(10L)
                .recommendationReason(RecommendationDetailResponse.RecommendationReason.builder()
                        .summary("Java 기반 추천")
                        .detail("Spring 심화 자료를 추천합니다.")
                        .build())
                .currentStatus(RecommendationDetailResponse.RecommendationCurrentStatus.builder()
                        .summary("학습 흐름이 좋습니다.")
                        .detail("Spring과 Java 중심으로 이어지고 있습니다.")
                        .topSkills(List.of("Spring", "Java"))
                        .build())
                .quizzes(List.of(
                        RecommendationDetailResponse.RecommendationQuiz.builder()
                                .title("Spring 핵심 개념 점검 퀴즈")
                                .description("추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.")
                                .expectedMinutes(15)
                                .totalQuestions(2)
                                .questions(List.of(
                                        RecommendationDetailResponse.RecommendationQuizQuestion.builder()
                                                .questionNumber(1)
                                                .question("Spring Bean의 기본 스코프는?")
                                                .quizType("MULTIPLE_CHOICE")
                                                .options(List.of("singleton", "prototype"))
                                                .build()
                                ))
                                .build()
                ))
                .references(List.of(
                        RecommendationDetailResponse.RecommendationReference.builder()
                                .title("Spring Boot 공식 문서")
                                .recommendationReason("공식 문서로 기초를 다지기 좋습니다.")
                                .referenceType("OFFICIAL_DOCS")
                                .publishedAt(LocalDate.of(2024, 6, 1))
                                .url("https://docs.spring.io/")
                                .build()
                ))
                .nextNodes(List.of(
                        RecommendationDetailResponse.RecommendationNextNode.builder()
                                .title("Spring Security 심화")
                                .build()
                ))
                .build();
        given(recommendationsService.findRecommendationDetail(1L, 10L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/recommendations/10").principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumId").value(10))
                .andExpect(jsonPath("$.recommendationReason.summary").value("Java 기반 추천"))
                .andExpect(jsonPath("$.recommendationReason.detail").value("Spring 심화 자료를 추천합니다."))
                .andExpect(jsonPath("$.currentStatus.summary").value("학습 흐름이 좋습니다."))
                .andExpect(jsonPath("$.currentStatus.detail").value("Spring과 Java 중심으로 이어지고 있습니다."))
                .andExpect(jsonPath("$.currentStatus.topSkills[0]").value("Spring"))
                .andExpect(jsonPath("$.quizzes[0].title").value("Spring 핵심 개념 점검 퀴즈"))
                .andExpect(jsonPath("$.quizzes[0].expectedMinutes").value(15))
                .andExpect(jsonPath("$.quizzes[0].totalQuestions").value(2))
                .andExpect(jsonPath("$.quizzes[0].questions[0].questionNumber").value(1))
                .andExpect(jsonPath("$.quizzes[0].questions[0].question").value("Spring Bean의 기본 스코프는?"))
                .andExpect(jsonPath("$.quizzes[0].questions[0].quizType").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.quizzes[0].questions[0].correctAnswer").doesNotExist())
                .andExpect(jsonPath("$.references[0].referenceType").value("OFFICIAL_DOCS"))
                .andExpect(jsonPath("$.nextNodes[0].title").value("Spring Security 심화"));
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
