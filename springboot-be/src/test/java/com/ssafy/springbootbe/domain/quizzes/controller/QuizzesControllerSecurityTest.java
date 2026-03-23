package com.ssafy.springbootbe.domain.quizzes.controller;

import com.ssafy.springbootbe.common.config.AuthenticationFailureHandler;
import com.ssafy.springbootbe.common.config.SecurityConfig;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.securityFilter.JwtVerificationFilter;
import com.ssafy.springbootbe.domain.quizzes.service.QuizzesService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(QuizzesController.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({SecurityConfig.class, JwtVerificationFilter.class, AuthenticationFailureHandler.class})
class QuizzesControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuizzesService quizzesService;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        given(claims.get("userId")).willReturn(1L);
        given(claims.get("userId", Long.class)).willReturn(1L);
        given(claims.get("nickName", String.class)).willReturn("tester");
        given(claims.get("email", String.class)).willReturn("tester@example.com");
        given(jwtUtils.getClaims(anyString())).willReturn(claims);
    }

    @Test
    void 인증_없는_퀴즈_세션_시작_요청은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/quizzes/sessions")
                        .contentType("application/json")
                        .content("""
                                {
                                  "curriculumId": 10
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("INVALID_TOKEN"));
    }
}
