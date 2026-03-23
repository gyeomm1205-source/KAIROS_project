package com.ssafy.springbootbe.domain.calendar.service;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarClientServiceTest {

    // @Value 필드(googleClientId, googleClientSecret)는 빌더/파싱 메서드에서 사용하지 않으므로
    // null이어도 아래 테스트에는 영향 없음
    @InjectMocks
    private GoogleCalendarClientService googleCalendarClientService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    // ===== parseEventStartDate =====

    @Test
    void parseEventStartDate_종일_이벤트는_date_필드를_파싱한다() {
        // given
        Event event = new Event();
        event.setStart(new EventDateTime().setDate(new DateTime("2025-03-10")));

        // when
        LocalDate result = googleCalendarClientService.parseEventStartDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseEventStartDate_시간지정_이벤트는_dateTime을_KST로_변환한다() {
        // given
        // KST 2025-03-10 02:00:00 → LocalDate는 2025-03-10
        ZonedDateTime kstDateTime = ZonedDateTime.of(2025, 3, 10, 2, 0, 0, 0, KST);
        long epochMilli = kstDateTime.toInstant().toEpochMilli();

        Event event = new Event();
        event.setStart(new EventDateTime().setDateTime(new DateTime(epochMilli)));

        // when
        LocalDate result = googleCalendarClientService.parseEventStartDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseEventStartDate_자정_직전_KST_시간지정_이벤트는_날짜가_바뀌지_않는다() {
        // given
        // KST 2025-03-10 23:59:00 → LocalDate는 여전히 2025-03-10
        ZonedDateTime kstDateTime = ZonedDateTime.of(2025, 3, 10, 23, 59, 0, 0, KST);
        long epochMilli = kstDateTime.toInstant().toEpochMilli();

        Event event = new Event();
        event.setStart(new EventDateTime().setDateTime(new DateTime(epochMilli)));

        // when
        LocalDate result = googleCalendarClientService.parseEventStartDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    // ===== parseEventEndDate =====

    @Test
    void parseEventEndDate_종일_이벤트는_exclusive_end에서_하루를_뺀다() {
        // given
        // Google Calendar 종일 이벤트의 end는 exclusive
        // endDate "2025-03-11" → 실제 마지막 날은 2025-03-10
        Event event = new Event();
        event.setEnd(new EventDateTime().setDate(new DateTime("2025-03-11")));

        // when
        LocalDate result = googleCalendarClientService.parseEventEndDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseEventEndDate_종일_이벤트_단일일은_시작일과_같은_날이_된다() {
        // given
        // 단일 날짜 종일 이벤트: start="2025-03-10", end="2025-03-11" → endDate = 2025-03-10
        Event event = new Event();
        event.setEnd(new EventDateTime().setDate(new DateTime("2025-03-11")));

        // when
        LocalDate result = googleCalendarClientService.parseEventEndDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseEventEndDate_시간지정_이벤트는_dateTime을_KST로_변환한다() {
        // given
        // KST 2025-03-10 18:00:00 → LocalDate는 2025-03-10
        ZonedDateTime kstDateTime = ZonedDateTime.of(2025, 3, 10, 18, 0, 0, 0, KST);
        long epochMilli = kstDateTime.toInstant().toEpochMilli();

        Event event = new Event();
        event.setEnd(new EventDateTime().setDateTime(new DateTime(epochMilli)));

        // when
        LocalDate result = googleCalendarClientService.parseEventEndDate(event);

        // then
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }

    @Test
    void parseEventEndDate_UTC_자정인_시간지정_이벤트는_KST로_다음날이_된다() {
        // given
        // UTC 2025-03-10 00:00:00 = KST 2025-03-10 09:00:00 → LocalDate는 2025-03-10
        ZonedDateTime utcDateTime = ZonedDateTime.of(2025, 3, 10, 0, 0, 0, 0, ZoneId.of("UTC"));
        long epochMilli = utcDateTime.toInstant().toEpochMilli();

        Event event = new Event();
        event.setEnd(new EventDateTime().setDateTime(new DateTime(epochMilli)));

        // when
        LocalDate result = googleCalendarClientService.parseEventEndDate(event);

        // then
        // KST로 변환하면 2025-03-10 09:00:00 → 날짜는 2025-03-10
        assertThat(result).isEqualTo(LocalDate.of(2025, 3, 10));
    }
}