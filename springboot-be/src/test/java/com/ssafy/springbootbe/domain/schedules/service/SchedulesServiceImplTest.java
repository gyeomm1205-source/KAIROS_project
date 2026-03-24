package com.ssafy.springbootbe.domain.schedules.service;

import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.service.GoogleCalendarClientService;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleCreateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.request.ScheduleUpdateRequest;
import com.ssafy.springbootbe.domain.schedules.dto.response.ScheduleResponse;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleAccessDeniedException;
import com.ssafy.springbootbe.domain.schedules.exception.ScheduleNotFoundException;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.schedule.entity.UserSchedule;
import com.ssafy.springbootbe.persistence.schedule.repository.UserScheduleRepository;
import com.ssafy.springbootbe.persistence.user.entity.User;
import com.ssafy.springbootbe.persistence.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SchedulesServiceImplTest {

    @Mock
    private UserScheduleRepository userScheduleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisService redisService;
    @Mock
    private OAuthAccountRepository oAuthAccountRepository;
    @Mock
    private GoogleCalendarClientService googleCalendarClientService;
    @Mock
    private OAuthTokenCryptoService oAuthTokenCryptoService;

    @InjectMocks
    private SchedulesServiceImpl schedulesService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .build();
    }

    // ===== createSchedule =====

    @Test
    void createSchedule_성공() {
        // given
        ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                .title("중간고사")
                .description("전공 시험")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        UserSchedule savedSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .title("중간고사")
                .description("전공 시험")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
        given(userScheduleRepository.save(any(UserSchedule.class))).willReturn(savedSchedule);

        // when
        ScheduleResponse response = schedulesService.createSchedule(1L, request);

        // then
        assertThat(response.getScheduleId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("중간고사");
        assertThat(response.getGoogleEventId()).isNull();
    }

    @Test
    void createSchedule_실패_종료일이_시작일보다_앞선_경우() {
        // given
        ScheduleCreateRequest request = ScheduleCreateRequest.builder()
                .title("중간고사")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 9))
                .build();

        // when & then
        assertThatThrownBy(() -> schedulesService.createSchedule(1L, request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ===== updateSchedule =====

    @Test
    void updateSchedule_성공() {
        // given
        UserSchedule existingSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .title("중간고사")
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        ScheduleUpdateRequest request = ScheduleUpdateRequest.builder()
                .title("기말고사")
                .startDate(LocalDate.of(2025, 2, 5))
                .endDate(LocalDate.of(2025, 2, 5))
                .build();

        given(userScheduleRepository.findById(1L)).willReturn(Optional.of(existingSchedule));

        // when
        ScheduleResponse response = schedulesService.updateSchedule(1L, 1L, request);

        // then
        assertThat(response.getTitle()).isEqualTo("기말고사");
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2025, 2, 5));
    }

    @Test
    void updateSchedule_실패_일정_없음() {
        // given
        given(userScheduleRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> schedulesService.updateSchedule(1L, 99L, new ScheduleUpdateRequest()))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    void updateSchedule_실패_본인_일정_아님() {
        // given
        User otherUser = User.builder().userId(2L).build();
        UserSchedule otherSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(otherUser)
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        given(userScheduleRepository.findById(1L)).willReturn(Optional.of(otherSchedule));

        // when & then
        assertThatThrownBy(() -> schedulesService.updateSchedule(1L, 1L, new ScheduleUpdateRequest()))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }

    // ===== deleteSchedule =====

    @Test
    void deleteSchedule_성공() {
        // given
        UserSchedule schedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        given(userScheduleRepository.findById(1L)).willReturn(Optional.of(schedule));

        // when
        schedulesService.deleteSchedule(1L, 1L);

        // then
        verify(userScheduleRepository).delete(schedule);
    }

    @Test
    void deleteSchedule_실패_일정_없음() {
        // given
        given(userScheduleRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> schedulesService.deleteSchedule(1L, 99L))
                .isInstanceOf(ScheduleNotFoundException.class);
    }

    @Test
    void deleteSchedule_실패_본인_일정_아님() {
        // given
        User otherUser = User.builder().userId(2L).build();
        UserSchedule otherSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(otherUser)
                .startDate(LocalDate.of(2025, 1, 10))
                .endDate(LocalDate.of(2025, 1, 10))
                .build();

        given(userScheduleRepository.findById(1L)).willReturn(Optional.of(otherSchedule));

        // when & then
        assertThatThrownBy(() -> schedulesService.deleteSchedule(1L, 1L))
                .isInstanceOf(ScheduleAccessDeniedException.class);
    }
}
