package com.ssafy.springbootbe.domain.analysis.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.analysis.dto.request.AnalysisCompleteRequest;
import com.ssafy.springbootbe.domain.analysis.dto.response.AnalysisCompleteResponse;
import com.ssafy.springbootbe.domain.analysis.service.AnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new AnalysisController(analysisService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ===== POST /analysis/complete =====

    @Test
    void 분석_완료_콜백_성공() throws Exception {
        // given
        AnalysisCompleteRequest request = new AnalysisCompleteRequest();

        AnalysisCompleteResponse response = AnalysisCompleteResponse.builder()
                .message("activity_history saved.")
                .savedCount(3)
                .build();

        given(analysisService.complete(any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/analysis/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("activity_history saved."))
                .andExpect(jsonPath("$.savedCount").value(3));
    }

    @Test
    void 분석_완료_콜백_실패_존재하지_않는_userId() throws Exception {
        // given
        given(analysisService.complete(any()))
                .willThrow(new IllegalArgumentException("존재하지 않는 userId=99"));

        // when & then
        mockMvc.perform(post("/analysis/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }
}