package com.ssafy.springbootbe.domain.calendar.controller;

import tools.jackson.databind.ObjectMapper;
import com.ssafy.springbootbe.common.dto.LoginUserPrincipal;
import com.ssafy.springbootbe.common.exception.GlobalExceptionHandler;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarConflictInfo;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarExportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarImportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarPeriod;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.exception.GoogleOAuthNotFoundException;
import com.ssafy.springbootbe.domain.calendar.service.CalendarService;
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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CalendarControllerTest {

    @Mock
    private CalendarService calendarService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(new CalendarController(calendarService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new LoginUserPrincipalArgumentResolver())
                .build();
    }

    // ===== GET /calendar =====

    @Test
    void getCalendar_성공() throws Exception {
        // given
        CalendarResponse response = CalendarResponse.builder()
                .period(CalendarPeriod.builder()
                        .start(LocalDate.of(2025, 3, 1))
                        .end(LocalDate.of(2025, 3, 31))
                        .build())
                .curricula(List.of())
                .personalSchedules(List.of())
                .build();

        given(calendarService.getCalendar(USER_ID, 2025, 3)).willReturn(response);

        // when & then
        mockMvc.perform(get("/calendar")
                        .principal(authenticatedUser())
                        .param("year", "2025")
                        .param("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.start").value("2025-03-01"))
                .andExpect(jsonPath("$.period.end").value("2025-03-31"))
                .andExpect(jsonPath("$.curricula").isArray())
                .andExpect(jsonPath("$.personalSchedules").isArray());
    }

    @Test
    void getCalendar_실패_구글_OAuth_없음() throws Exception {
        // given
        given(calendarService.getCalendar(eq(USER_ID), eq(2025), eq(3)))
                .willThrow(new GoogleOAuthNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(get("/calendar")
                        .principal(authenticatedUser())
                        .param("year", "2025")
                        .param("month", "3"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== POST /calendar/sync/export =====

    @Test
    void exportToGoogle_성공() throws Exception {
        // given
        CalendarExportResponse response = CalendarExportResponse.builder()
                .exportedNodes(3)
                .exportedSchedules(2)
                .build();

        given(calendarService.export(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(post("/calendar/sync/export")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exportedNodes").value(3))
                .andExpect(jsonPath("$.exportedSchedules").value(2));
    }

    @Test
    void exportToGoogle_실패_구글_OAuth_없음() throws Exception {
        // given
        given(calendarService.export(USER_ID))
                .willThrow(new GoogleOAuthNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(post("/calendar/sync/export")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    // ===== POST /calendar/sync/import =====

    @Test
    void importFromGoogle_성공() throws Exception {
        // given
        CalendarImportResponse response = CalendarImportResponse.builder()
                .conflictNodes(List.of())
                .updatedSchedules(1)
                .newSchedules(2)
                .build();

        given(calendarService.importFromGoogle(USER_ID)).willReturn(response);

        // when & then
        mockMvc.perform(post("/calendar/sync/import")
                        .principal(authenticatedUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedSchedules").value(1))
                .andExpect(jsonPath("$.newSchedules").value(2))
                .andExpect(jsonPath("$.conflictNodes").isArray());
    }

    @Test
    void importFromGoogle_실패_구글_OAuth_없음() throws Exception {
        // given
        given(calendarService.importFromGoogle(USER_ID))
                .willThrow(new GoogleOAuthNotFoundException(USER_ID));

        // when & then
        mockMvc.perform(post("/calendar/sync/import")
                        .principal(authenticatedUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
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