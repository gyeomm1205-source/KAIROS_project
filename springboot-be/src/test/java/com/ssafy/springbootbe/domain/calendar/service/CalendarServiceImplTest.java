package com.ssafy.springbootbe.domain.calendar.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.ssafy.springbootbe.common.redis.RedisService;
import com.ssafy.springbootbe.common.utils.OAuthTokenCryptoService;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;
import com.ssafy.springbootbe.domain.calendar.exception.GoogleOAuthNotFoundException;
import com.ssafy.springbootbe.persistence.curriculum.entity.Curriculum;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNode;
import com.ssafy.springbootbe.persistence.curriculum.entity.CurriculumNodeCalendarSync;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeCalendarSyncRepository;
import com.ssafy.springbootbe.persistence.curriculum.repository.CurriculumNodeRepository;
import com.ssafy.springbootbe.persistence.curriculum.type.CurriculumStatus;
import com.ssafy.springbootbe.persistence.curriculum.type.SyncStatus;
import com.ssafy.springbootbe.persistence.oauth.entity.OAuthAccount;
import com.ssafy.springbootbe.persistence.oauth.repository.OAuthAccountRepository;
import com.ssafy.springbootbe.persistence.oauth.type.OAuthProvider;
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

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CalendarServiceImplTest {

    @Mock private CurriculumNodeRepository curriculumNodeRepository;
    @Mock private UserScheduleRepository userScheduleRepository;
    @Mock private CurriculumNodeCalendarSyncRepository curriculumNodeCalendarSyncRepository;
    @Mock private OAuthAccountRepository oAuthAccountRepository;
    @Mock private UserRepository userRepository;
    @Mock private RedisService redisService;
    @Mock private ObjectMapper objectMapper;
    @Mock private GoogleCalendarClientService googleCalendarClientService;
    @Mock private OAuthTokenCryptoService oAuthTokenCryptoService;

    @InjectMocks
    private CalendarServiceImpl calendarService;

    private static final Long USER_ID = 1L;
    private static final String CACHE_KEY_MARCH = "calendar:1:2025:3";

    private User mockUser;
    private OAuthAccount mockOAuthAccount;
    private Curriculum mockCurriculum;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(USER_ID)
                .email("test@test.com")
                .nickname("tester")
                .build();

        mockOAuthAccount = OAuthAccount.builder()
                .oauthAccountId(1L)
                .user(mockUser)
                .provider(OAuthProvider.GOOGLE)
                .providerAccountId("google_sub_123")
                .refreshToken("encrypted_refresh_token")
                .build();

        mockCurriculum = Curriculum.builder()
                .curriculumId(1L)
                .user(mockUser)
                .status(CurriculumStatus.ACTIVE)
                .duration(5)
                .build();
    }

    // ===== getCalendar =====

    @Test
    void getCalendar_캐시_히트시_DB를_조회하지_않는다() throws Exception {
        // given
        CalendarResponse cachedResponse = CalendarResponse.builder()
                .curricula(List.of())
                .personalSchedules(List.of())
                .build();

        given(redisService.get(CACHE_KEY_MARCH)).willReturn("{\"cached\":true}");
        given(objectMapper.readValue("{\"cached\":true}", CalendarResponse.class)).willReturn(cachedResponse);

        // when
        CalendarResponse result = calendarService.getCalendar(USER_ID, 2025, 3);

        // then
        assertThat(result).isEqualTo(cachedResponse);
        verify(curriculumNodeRepository, never())
                .findByCurriculumUserUserIdAndScheduledDateBetween(any(), any(), any());
        verify(userScheduleRepository, never())
                .findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(any(), any(), any());
    }

    @Test
    void getCalendar_캐시_미스시_DB를_조회하고_응답을_반환한다() {
        // given
        given(redisService.get(CACHE_KEY_MARCH)).willReturn(null);
        given(curriculumNodeRepository.findByCurriculumUserUserIdAndScheduledDateBetween(
                eq(USER_ID), eq(LocalDate.of(2025, 3, 1)), eq(LocalDate.of(2025, 3, 31))))
                .willReturn(List.of());
        given(userScheduleRepository.findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(USER_ID), eq(LocalDate.of(2025, 3, 31)), eq(LocalDate.of(2025, 3, 1))))
                .willReturn(List.of());

        // when
        CalendarResponse result = calendarService.getCalendar(USER_ID, 2025, 3);

        // then
        assertThat(result.getPeriod().getStart()).isEqualTo(LocalDate.of(2025, 3, 1));
        assertThat(result.getPeriod().getEnd()).isEqualTo(LocalDate.of(2025, 3, 31));
        assertThat(result.getCurricula()).isEmpty();
        assertThat(result.getPersonalSchedules()).isEmpty();
    }

    @Test
    void getCalendar_캐시_역직렬화_실패시_DB를_재조회한다() throws Exception {
        // given
        // 캐시 값은 있지만 역직렬화에 실패하면 DB를 재조회해야 한다
        given(redisService.get(CACHE_KEY_MARCH)).willReturn("{invalid_json}");
        given(objectMapper.readValue(anyString(), eq(CalendarResponse.class)))
                .willThrow(mock(JacksonException.class));
        given(curriculumNodeRepository.findByCurriculumUserUserIdAndScheduledDateBetween(
                eq(USER_ID), eq(LocalDate.of(2025, 3, 1)), eq(LocalDate.of(2025, 3, 31))))
                .willReturn(List.of());
        given(userScheduleRepository.findByUserUserIdAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                eq(USER_ID), eq(LocalDate.of(2025, 3, 31)), eq(LocalDate.of(2025, 3, 1))))
                .willReturn(List.of());

        // when
        CalendarResponse result = calendarService.getCalendar(USER_ID, 2025, 3);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getPeriod().getStart()).isEqualTo(LocalDate.of(2025, 3, 1));
        verify(curriculumNodeRepository)
                .findByCurriculumUserUserIdAndScheduledDateBetween(any(), any(), any());
    }

    // ===== export =====

    @Test
    void export_실패_구글_OAuth_계정_없음() {
        // given
        given(oAuthAccountRepository.findByUserUserIdAndProvider(USER_ID, OAuthProvider.GOOGLE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> calendarService.export(USER_ID))
                .isInstanceOf(GoogleOAuthNotFoundException.class);
    }

    @Test
    void export_미동기화_노드를_구글_캘린더에_새로_생성한다() throws IOException {
        // given
        stubGoogleCalendarClient();

        CurriculumNode node = CurriculumNode.builder()
                .curriculumNodeId(1L)
                .curriculum(mockCurriculum)
                .title("Spring 기초 학습")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .build();
        CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(1L)
                .curriculumNode(node)
                .syncStatus(SyncStatus.NOT_SYNCED)
                .build(); // googleEventId가 null → create 경로

        Event createdEvent = new Event().setId("google-event-001").setEtag("etag-001");

        given(curriculumNodeCalendarSyncRepository
                .findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(USER_ID, SyncStatus.NOT_SYNCED))
                .willReturn(List.of(sync));
        given(userScheduleRepository.findByUserUserIdAndGoogleEventIdIsNull(USER_ID))
                .willReturn(List.of());
        given(googleCalendarClientService.createAllDayEvent(
                any(Calendar.class), eq("Spring 기초 학습"), nullable(String.class), eq(LocalDate.of(2025, 3, 10))))
                .willReturn(createdEvent);

        // when
        var response = calendarService.export(USER_ID);

        // then
        assertThat(response.getExportedNodes()).isEqualTo(1);
        assertThat(response.getExportedSchedules()).isEqualTo(0);
        assertThat(sync.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(sync.getGoogleEventId()).isEqualTo("google-event-001");
    }

    @Test
    void export_기존_이벤트ID가_있는_노드는_구글_캘린더를_업데이트한다() throws IOException {
        // given
        stubGoogleCalendarClient();

        CurriculumNode node = CurriculumNode.builder()
                .curriculumNodeId(2L)
                .curriculum(mockCurriculum)
                .title("JPA 심화 학습")
                .scheduledDate(LocalDate.of(2025, 3, 15))
                .build();
        CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(2L)
                .curriculumNode(node)
                .googleEventId("existing-event-999")
                .syncStatus(SyncStatus.NOT_SYNCED)
                .build(); // googleEventId가 존재 → update 경로

        Event updatedEvent = new Event().setId("existing-event-999").setEtag("etag-updated");

        given(curriculumNodeCalendarSyncRepository
                .findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(USER_ID, SyncStatus.NOT_SYNCED))
                .willReturn(List.of(sync));
        given(userScheduleRepository.findByUserUserIdAndGoogleEventIdIsNull(USER_ID))
                .willReturn(List.of());
        given(googleCalendarClientService.updateAllDayEvent(
                any(Calendar.class), eq("existing-event-999"),
                eq("JPA 심화 학습"), nullable(String.class), eq(LocalDate.of(2025, 3, 15))))
                .willReturn(updatedEvent);

        // when
        var response = calendarService.export(USER_ID);

        // then
        assertThat(response.getExportedNodes()).isEqualTo(1);
        assertThat(sync.getSyncStatus()).isEqualTo(SyncStatus.SYNCED);
        assertThat(sync.getGoogleEventId()).isEqualTo("existing-event-999");
    }

    @Test
    void export_미링크_개인일정을_구글_캘린더에_내보낸다() throws IOException {
        // given
        stubGoogleCalendarClient();

        UserSchedule schedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .title("중간고사")
                .startDate(LocalDate.of(2025, 3, 20))
                .endDate(LocalDate.of(2025, 3, 21))
                .build(); // googleEventId가 null

        Event createdEvent = new Event().setId("schedule-event-abc");

        given(curriculumNodeCalendarSyncRepository
                .findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(USER_ID, SyncStatus.NOT_SYNCED))
                .willReturn(List.of());
        given(userScheduleRepository.findByUserUserIdAndGoogleEventIdIsNull(USER_ID))
                .willReturn(List.of(schedule));
        given(googleCalendarClientService.createAllDayEvent(
                any(Calendar.class), eq("중간고사"), nullable(String.class),
                eq(LocalDate.of(2025, 3, 20)), eq(LocalDate.of(2025, 3, 21))))
                .willReturn(createdEvent);

        // when
        var response = calendarService.export(USER_ID);

        // then
        assertThat(response.getExportedNodes()).isEqualTo(0);
        assertThat(response.getExportedSchedules()).isEqualTo(1);
        assertThat(schedule.getGoogleEventId()).isEqualTo("schedule-event-abc");
    }

    @Test
    void export_노드_내보내기_IOException_발생시_SYNC_FAILED_처리하고_계속_진행한다() throws IOException {
        // given
        stubGoogleCalendarClient();

        CurriculumNode failNode = CurriculumNode.builder()
                .curriculumNodeId(1L)
                .curriculum(mockCurriculum)
                .title("실패 노드")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .build();
        CurriculumNodeCalendarSync failSync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(1L)
                .curriculumNode(failNode)
                .syncStatus(SyncStatus.NOT_SYNCED)
                .build();

        given(curriculumNodeCalendarSyncRepository
                .findByCurriculumNodeCurriculumUserUserIdAndSyncStatus(USER_ID, SyncStatus.NOT_SYNCED))
                .willReturn(List.of(failSync));
        given(userScheduleRepository.findByUserUserIdAndGoogleEventIdIsNull(USER_ID))
                .willReturn(List.of());
        given(googleCalendarClientService.createAllDayEvent(
                any(Calendar.class), any(String.class), nullable(String.class), any(LocalDate.class)))
                .willThrow(new IOException("Google API 네트워크 오류"));

        // when
        var response = calendarService.export(USER_ID);

        // then
        assertThat(response.getExportedNodes()).isEqualTo(0); // 실패했으므로 카운트 안 됨
        assertThat(failSync.getSyncStatus()).isEqualTo(SyncStatus.SYNC_FAILED);
    }

    // ===== importFromGoogle =====

    @Test
    void importFromGoogle_실패_구글_OAuth_계정_없음() {
        // given
        given(oAuthAccountRepository.findByUserUserIdAndProvider(USER_ID, OAuthProvider.GOOGLE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> calendarService.importFromGoogle(USER_ID))
                .isInstanceOf(GoogleOAuthNotFoundException.class);
    }

    @Test
    void importFromGoogle_실패_이벤트_조회_IOException시_IllegalStateException_발생() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));
        given(googleCalendarClientService.listAllEvents(any(Calendar.class)))
                .willThrow(new IOException("Google API 조회 실패"));

        // when & then
        assertThatThrownBy(() -> calendarService.importFromGoogle(USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google Calendar 이벤트 조회 실패");
    }

    @Test
    void importFromGoogle_신규_이벤트를_UserSchedule로_저장한다() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

        Event newEvent = buildAllDayEvent("event-new-001", "etag-001", "코딩 테스트 준비",
                "2025-03-10", "2025-03-11");

        given(googleCalendarClientService.listAllEvents(any(Calendar.class))).willReturn(List.of(newEvent));
        given(curriculumNodeCalendarSyncRepository.findByGoogleEventId("event-new-001"))
                .willReturn(Optional.empty());
        given(userScheduleRepository.findByGoogleEventId("event-new-001"))
                .willReturn(Optional.empty());
        given(googleCalendarClientService.parseEventStartDate(newEvent)).willReturn(LocalDate.of(2025, 3, 10));
        given(googleCalendarClientService.parseEventEndDate(newEvent)).willReturn(LocalDate.of(2025, 3, 10));

        // when
        var response = calendarService.importFromGoogle(USER_ID);

        // then
        assertThat(response.getNewSchedules()).isEqualTo(1);
        assertThat(response.getUpdatedSchedules()).isEqualTo(0);
        assertThat(response.getConflictNodes()).isEmpty();
        verify(userScheduleRepository).save(any(UserSchedule.class));
    }

    @Test
    void importFromGoogle_기존_UserSchedule_제목_변경시_업데이트한다() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

        UserSchedule existingSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .title("기존 제목")
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 3, 10))
                .googleEventId("event-existing-001")
                .build();

        Event updatedEvent = buildAllDayEvent("event-existing-001", "etag-changed", "새 제목",
                "2025-03-10", "2025-03-11");

        given(googleCalendarClientService.listAllEvents(any(Calendar.class))).willReturn(List.of(updatedEvent));
        given(curriculumNodeCalendarSyncRepository.findByGoogleEventId("event-existing-001"))
                .willReturn(Optional.empty());
        given(userScheduleRepository.findByGoogleEventId("event-existing-001"))
                .willReturn(Optional.of(existingSchedule));
        given(googleCalendarClientService.parseEventStartDate(updatedEvent)).willReturn(LocalDate.of(2025, 3, 10));
        given(googleCalendarClientService.parseEventEndDate(updatedEvent)).willReturn(LocalDate.of(2025, 3, 10));

        // when
        var response = calendarService.importFromGoogle(USER_ID);

        // then
        assertThat(response.getUpdatedSchedules()).isEqualTo(1);
        assertThat(response.getNewSchedules()).isEqualTo(0);
        assertThat(existingSchedule.getTitle()).isEqualTo("새 제목");
    }

    @Test
    void importFromGoogle_기존_UserSchedule_변경없으면_스킵한다() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

        UserSchedule unchangedSchedule = UserSchedule.builder()
                .userScheduleId(1L)
                .user(mockUser)
                .title("변경없는 일정")
                .startDate(LocalDate.of(2025, 3, 10))
                .endDate(LocalDate.of(2025, 3, 10))
                .googleEventId("event-same-001")
                .build();

        Event sameEvent = buildAllDayEvent("event-same-001", "etag-same", "변경없는 일정",
                "2025-03-10", "2025-03-11");

        given(googleCalendarClientService.listAllEvents(any(Calendar.class))).willReturn(List.of(sameEvent));
        given(curriculumNodeCalendarSyncRepository.findByGoogleEventId("event-same-001"))
                .willReturn(Optional.empty());
        given(userScheduleRepository.findByGoogleEventId("event-same-001"))
                .willReturn(Optional.of(unchangedSchedule));
        given(googleCalendarClientService.parseEventStartDate(sameEvent)).willReturn(LocalDate.of(2025, 3, 10));
        given(googleCalendarClientService.parseEventEndDate(sameEvent)).willReturn(LocalDate.of(2025, 3, 10));

        // when
        var response = calendarService.importFromGoogle(USER_ID);

        // then
        assertThat(response.getUpdatedSchedules()).isEqualTo(0);
        assertThat(response.getNewSchedules()).isEqualTo(0);
    }

    @Test
    void importFromGoogle_이태그_불일치시_커리큘럼_노드_충돌로_기록한다() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

        CurriculumNode node = CurriculumNode.builder()
                .curriculumNodeId(1L)
                .curriculum(mockCurriculum)
                .title("Spring 학습")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .build();
        CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(1L)
                .curriculumNode(node)
                .googleEventId("event-node-001")
                .googleEtag("old-etag")
                .syncStatus(SyncStatus.SYNCED)
                .build();

        // Google에서 etag가 변경된 이벤트가 들어옴
        Event modifiedEvent = buildAllDayEvent("event-node-001", "new-etag", "수정된 Spring 학습",
                "2025-03-10", "2025-03-11");

        given(googleCalendarClientService.listAllEvents(any(Calendar.class))).willReturn(List.of(modifiedEvent));
        given(curriculumNodeCalendarSyncRepository.findByGoogleEventId("event-node-001"))
                .willReturn(Optional.of(sync));
        given(googleCalendarClientService.parseEventStartDate(modifiedEvent)).willReturn(LocalDate.of(2025, 3, 10));

        // when
        var response = calendarService.importFromGoogle(USER_ID);

        // then
        assertThat(response.getConflictNodes()).hasSize(1);
        assertThat(response.getConflictNodes().get(0).getCurriculumNodeId()).isEqualTo(1L);
        assertThat(response.getConflictNodes().get(0).getGoogleTitle()).isEqualTo("수정된 Spring 학습");
        assertThat(sync.getGoogleEtag()).isEqualTo("new-etag"); // etag 업데이트 확인
    }

    @Test
    void importFromGoogle_이태그_동일시_충돌없이_스킵한다() throws IOException {
        // given
        stubGoogleCalendarClient();
        given(userRepository.findById(USER_ID)).willReturn(Optional.of(mockUser));

        CurriculumNode node = CurriculumNode.builder()
                .curriculumNodeId(1L)
                .curriculum(mockCurriculum)
                .title("Spring 학습")
                .scheduledDate(LocalDate.of(2025, 3, 10))
                .build();
        CurriculumNodeCalendarSync sync = CurriculumNodeCalendarSync.builder()
                .curriculumNodeCalendarSyncId(1L)
                .curriculumNode(node)
                .googleEventId("event-node-002")
                .googleEtag("same-etag")
                .syncStatus(SyncStatus.SYNCED)
                .build();

        // etag가 동일 → 변경 없음
        Event sameEvent = buildAllDayEvent("event-node-002", "same-etag", "Spring 학습",
                "2025-03-10", "2025-03-11");

        given(googleCalendarClientService.listAllEvents(any(Calendar.class))).willReturn(List.of(sameEvent));
        given(curriculumNodeCalendarSyncRepository.findByGoogleEventId("event-node-002"))
                .willReturn(Optional.of(sync));

        // when
        var response = calendarService.importFromGoogle(USER_ID);

        // then
        assertThat(response.getConflictNodes()).isEmpty();
        assertThat(response.getUpdatedSchedules()).isEqualTo(0);
    }

    // ===== helpers =====

    private void stubGoogleCalendarClient() {
        given(oAuthAccountRepository.findByUserUserIdAndProvider(USER_ID, OAuthProvider.GOOGLE))
                .willReturn(Optional.of(mockOAuthAccount));
        given(oAuthTokenCryptoService.decrypt("encrypted_refresh_token"))
                .willReturn("decrypted_refresh_token");
        given(googleCalendarClientService.buildCalendarClient("decrypted_refresh_token"))
                .willReturn(mock(Calendar.class));
    }

    private Event buildAllDayEvent(String eventId, String etag, String summary,
                                   String startDate, String endDate) {
        Event event = new Event()
                .setId(eventId)
                .setEtag(etag)
                .setSummary(summary);
        event.setStart(new EventDateTime().setDate(new DateTime(startDate)));
        event.setEnd(new EventDateTime().setDate(new DateTime(endDate)));
        return event;
    }
}
