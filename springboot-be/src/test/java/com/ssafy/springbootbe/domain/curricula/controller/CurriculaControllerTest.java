package com.ssafy.springbootbe.domain.curricula.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.curricula.dto.request.AnalysisDataRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumNodeUpdateRequest;
import com.ssafy.springbootbe.domain.curricula.dto.request.CurriculumPreviewRequest;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumConfirmResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumNodeResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumPreviewResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.CurriculumReasonResponse;
import com.ssafy.springbootbe.domain.curricula.dto.response.PreviewReasonDto;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeAccessDeniedException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNodeNotFoundException;
import com.ssafy.springbootbe.domain.curricula.exception.CurriculumNotFoundException;
import com.ssafy.springbootbe.domain.curricula.service.CurriculaService;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.curriculum.type.ProgressStatus;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CurriculaControllerTest {

    @Mock
    private CurriculaService curriculaService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new CurriculaController(curriculaService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    // ===== POST /curricula/preview =====

    @Test
    void 커리큘럼_미리보기_성공() throws Exception {
        // given
        CurriculumPreviewRequest request = CurriculumPreviewRequest.builder()
                .analysisData(AnalysisDataRequest.builder()
                        .summary("백엔드 중심으로 활동함")
                        .techDetails(List.of())
                        .recommendedPositions(List.of())
                        .build())
                .build();

        CurriculumPreviewResponse response = CurriculumPreviewResponse.builder()
                .curriculumPreviewKey("preview-uuid-1234")
                .duration(30)
                .recommendationReason(PreviewReasonDto.builder()
                        .summaryLine("Spring Boot 중심 커리큘럼")
                        .build())
                .nodes(List.of())
                .build();

        given(curriculaService.preview(eq(USER_ID), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/curricula/preview")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumPreviewKey").value("preview-uuid-1234"))
                .andExpect(jsonPath("$.duration").value(30))
                .andExpect(jsonPath("$.recommendationReason.summaryLine").value("Spring Boot 중심 커리큘럼"))
                .andExpect(jsonPath("$.nodes").isArray());
    }

    @Test
    void 커리큘럼_미리보기_실패_analysisData_없음() throws Exception {
        // given: analysisData = null
        String body = "{}";

        // when & then
        mockMvc.perform(post("/curricula/preview")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    // ===== POST /curricula/confirm =====

    @Test
    void 커리큘럼_확정_성공() throws Exception {
        // given
        CurriculumConfirmResponse response = CurriculumConfirmResponse.builder()
                .curriculumId(10L)
                .status(CurriculumStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2025, 3, 1, 9, 0))
                .nodes(List.of())
                .build();

        given(curriculaService.confirm(eq(USER_ID), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/curricula/confirm")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curriculumPreviewKey\": \"preview-uuid-1234\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.curriculumId").value(10))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.nodes").isArray());
    }

    @Test
    void 커리큘럼_확정_실패_previewKey_공백() throws Exception {
        // when & then
        mockMvc.perform(post("/curricula/confirm")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curriculumPreviewKey\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    @Test
    void 커리큘럼_확정_실패_커리큘럼_없음() throws Exception {
        // given
        given(curriculaService.confirm(eq(USER_ID), any()))
                .willThrow(new CurriculumNotFoundException(99L));

        // when & then
        mockMvc.perform(post("/curricula/confirm")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"curriculumPreviewKey\": \"preview-uuid-1234\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== PATCH /curricula/nodes/{nodeId} =====

    @Test
    void 커리큘럼_노드_수정_성공() throws Exception {
        // given
        CurriculumNodeResponse response = CurriculumNodeResponse.builder()
                .curriculumNodeId(5L)
                .curriculumId(1L)
                .title("Spring Security 학습")
                .description("JWT 인증 흐름 파악")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .expectedMinutes(60)
                .progressStatus(ProgressStatus.IN_PROGRESS)
                .build();

        given(curriculaService.updateNode(eq(USER_ID), eq(5L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/curricula/nodes/5")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressStatus\": \"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumNodeId").value(5))
                .andExpect(jsonPath("$.title").value("Spring Security 학습"))
                .andExpect(jsonPath("$.progressStatus").value("IN_PROGRESS"));
    }

    @Test
    void 커리큘럼_노드_수정_실패_노드_없음() throws Exception {
        // given
        given(curriculaService.updateNode(eq(USER_ID), eq(99L), any()))
                .willThrow(new CurriculumNodeNotFoundException(99L));

        // when & then
        mockMvc.perform(patch("/curricula/nodes/99")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressStatus\": \"DONE\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 커리큘럼_노드_수정_실패_권한_없음() throws Exception {
        // given
        given(curriculaService.updateNode(eq(USER_ID), eq(5L), any()))
                .willThrow(new CurriculumNodeAccessDeniedException(5L));

        // when & then
        mockMvc.perform(patch("/curricula/nodes/5")
                        .principal(authenticatedUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"progressStatus\": \"DONE\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    // ===== GET /curricula/{curriculumId}/reason =====

    @Test
    void 커리큘럼_추천_근거_조회_성공() throws Exception {
        // given
        CurriculumReasonResponse response = CurriculumReasonResponse.builder()
                .curriculumId(10L)
                .summaryLine("Spring Boot 중심 커리큘럼")
                .userContext("백엔드 학습 중심")
                .aiInterpretation("백엔드 역량 강화 필요")
                .curriculumRationale("Spring Boot 실습 위주로 구성")
                .createdAt(LocalDateTime.of(2025, 3, 1, 9, 0))
                .build();

        given(curriculaService.getReason(eq(USER_ID), eq(10L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/curricula/10/reason")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumId").value(10))
                .andExpect(jsonPath("$.summaryLine").value("Spring Boot 중심 커리큘럼"))
                .andExpect(jsonPath("$.userContext").value("백엔드 학습 중심"));
    }

    @Test
    void 커리큘럼_추천_근거_조회_실패_커리큘럼_없음() throws Exception {
        // given
        given(curriculaService.getReason(eq(USER_ID), eq(99L)))
                .willThrow(new CurriculumNotFoundException(99L));

        // when & then
        mockMvc.perform(get("/curricula/99/reason")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 커리큘럼_추천_근거_조회_실패_권한_없음() throws Exception {
        // given
        given(curriculaService.getReason(eq(USER_ID), eq(10L)))
                .willThrow(new CurriculumAccessDeniedException(10L));

        // when & then
        mockMvc.perform(get("/curricula/10/reason")
                        .principal(authenticatedUser()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    // ===== GET /curricula/nodes/{nodeId} =====

    @Test
    void 커리큘럼_노드_조회_성공() throws Exception {
        // given
        CurriculumNodeResponse response = CurriculumNodeResponse.builder()
                .curriculumNodeId(5L)
                .curriculumId(1L)
                .title("Spring Security 학습")
                .description("JWT 인증 흐름 파악")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .expectedMinutes(60)
                .progressStatus(ProgressStatus.NOT_STARTED)
                .build();

        given(curriculaService.getNode(eq(USER_ID), eq(5L))).willReturn(response);

        // when & then
        mockMvc.perform(get("/curricula/nodes/5")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.curriculumNodeId").value(5))
                .andExpect(jsonPath("$.title").value("Spring Security 학습"))
                .andExpect(jsonPath("$.progressStatus").value("NOT_STARTED"));
    }

    @Test
    void 커리큘럼_노드_조회_실패_노드_없음() throws Exception {
        // given
        given(curriculaService.getNode(eq(USER_ID), eq(99L)))
                .willThrow(new CurriculumNodeNotFoundException(99L));

        // when & then
        mockMvc.perform(get("/curricula/nodes/99")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 커리큘럼_노드_조회_실패_권한_없음() throws Exception {
        // given
        given(curriculaService.getNode(eq(USER_ID), eq(5L)))
                .willThrow(new CurriculumNodeAccessDeniedException(5L));

        // when & then
        mockMvc.perform(get("/curricula/nodes/5")
                        .principal(authenticatedUser()))
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
