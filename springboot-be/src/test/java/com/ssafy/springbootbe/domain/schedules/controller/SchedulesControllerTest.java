package com.ssafy.springbootbe.domain.schedules.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.jwt.JWTUtils;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleAccessDeniedException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleNotFoundException;
import com.ssafy.springbootbe.domain.schedules.service.SchedulesService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SchedulesController.class)
class SchedulesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SchedulesService schedulesService;

    @MockitoBean
    private JWTUtils jwtUtils;

    @MockitoBean
    private RedisService redisService;

    private static final String BEARER_TOKEN = "Bearer test-token";
    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        given(claims.get("userId")).willReturn(USER_ID);
        given(jwtUtils.getClaims("test-token")).willReturn(claims);
    }

    // ===== POST /schedules =====

    @Test
    void 일정_생성_성공() throws Exception {
        // given
        ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                .title("중간고사")
                .description("전공 시험")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        ScheduleResponse response = ScheduleResponse.builder()
                .scheduleId(1L)
                .title("중간고사")
                .description("전공 시험")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .googleEventId(null)
                .build();

        given(schedulesService.createSchedule(eq(USER_ID), any())).willReturn(response);

        // when & then
        mockMvc.perform(post("/schedules")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").value(1))
                .andExpect(jsonPath("$.title").value("중간고사"))
                .andExpect(jsonPath("$.googleEventId").doesNotExist());
    }

    @Test
    void 일정_생성_실패_제목_없음() throws Exception {
        // given
        ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        // when & then
        mockMvc.perform(post("/schedules")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_INPUT"));
    }

    // ===== PATCH /schedules/{id} =====

    @Test
    void 일정_수정_성공() throws Exception {
        // given
        ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
                .title("기말고사")
                .startDate(LocalDate.of(2025, 2, 5))
                .endDate(LocalDate.of(2025, 2, 5))
                .build();

        ScheduleResponse response = ScheduleResponse.builder()
                .scheduleId(1L)
                .title("기말고사")
                .startDate(LocalDate.of(2025, 2, 5))
                .endDate(LocalDate.of(2025, 2, 5))
                .build();

        given(schedulesService.updateSchedule(eq(USER_ID), eq(1L), any())).willReturn(response);

        // when & then
        mockMvc.perform(patch("/schedules/1")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("기말고사"));
    }

    @Test
    void 일정_수정_실패_일정_없음() throws Exception {
        // given
        given(schedulesService.updateSchedule(eq(USER_ID), eq(99L), any()))
                .willThrow(new ScheduleNotFoundException(99L));

        // when & then
        mockMvc.perform(patch("/schedules/99")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleUpdateRequest())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 일정_수정_실패_권한_없음() throws Exception {
        // given
        given(schedulesService.updateSchedule(eq(USER_ID), eq(1L), any()))
                .willThrow(new ScheduleAccessDeniedException(1L));

        // when & then
        mockMvc.perform(patch("/schedules/1")
                        .header("Authorization", BEARER_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ScheduleUpdateRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }

    // ===== DELETE /schedules/{id} =====

    @Test
    void 일정_삭제_성공() throws Exception {
        // when & then
        mockMvc.perform(delete("/schedules/1")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isNoContent());
    }

    @Test
    void 일정_삭제_실패_일정_없음() throws Exception {
        // given
        doThrow(new ScheduleNotFoundException(99L))
                .when(schedulesService).deleteSchedule(USER_ID, 99L);

        // when & then
        mockMvc.perform(delete("/schedules/99")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void 일정_삭제_실패_권한_없음() throws Exception {
        // given
        doThrow(new ScheduleAccessDeniedException(1L))
                .when(schedulesService).deleteSchedule(USER_ID, 1L);

        // when & then
        mockMvc.perform(delete("/schedules/1")
                        .header("Authorization", BEARER_TOKEN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ACCESS_DENIED"));
    }
}
