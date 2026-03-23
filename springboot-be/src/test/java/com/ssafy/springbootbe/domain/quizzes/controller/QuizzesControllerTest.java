package com.ssafy.springbootbe.domain.quizzes.controller;

import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.quizzes.dto.request.QuizSessionStartRequest;
import com.ssafy.springbootbe.domain.quizzes.dto.response.QuizSessionStartResponse;
import com.ssafy.springbootbe.domain.quizzes.service.QuizzesService;
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
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuizzesControllerTest {

    @Mock
    private QuizzesService quizzesService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new QuizzesController(quizzesService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    @Test
    void 퀴즈_세션_시작_응답_필드명이_camelCase와_일치한다() throws Exception {
        // given
        QuizSessionStartResponse response = QuizSessionStartResponse.builder()
                .curriculumId(10L)
                .totalQuestions(20)
                .title("Spring 핵심 개념 점검 퀴즈")
                .description("추천 탭에서 바로 풀어볼 수 있는 Spring 중심 사전 생성 퀴즈입니다.")
                .expectedMinutes(15)
                .questions(List.of(
                        QuizSessionStartResponse.Question.builder()
                                .questionNumber(1)
                                .question("Spring Bean의 기본 스코프는?")
                                .quizType("MULTIPLE_CHOICE")
                                .options(List.of("singleton", "prototype", "request", "session"))
                                .build()
                ))
                .build();
        given(quizzesService.startSession(eq(1L), any(QuizSessionStartRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/quizzes/sessions")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuizSessionStartRequest(10L))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curriculumId").value(10))
                .andExpect(jsonPath("$.totalQuestions").value(20))
                .andExpect(jsonPath("$.expectedMinutes").value(15))
                .andExpect(jsonPath("$.questions[0].questionNumber").value(1))
                .andExpect(jsonPath("$.questions[0].question").value("Spring Bean의 기본 스코프는?"))
                .andExpect(jsonPath("$.questions[0].quizType").value("MULTIPLE_CHOICE"))
                .andExpect(jsonPath("$.questions[0].correctAnswer").doesNotExist());
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
