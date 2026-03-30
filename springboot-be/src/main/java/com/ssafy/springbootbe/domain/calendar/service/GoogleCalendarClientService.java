package com.ssafy.springbootbe.domain.calendar.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class GoogleCalendarClientService {

    private static final String APPLICATION_NAME = "KAIROS";
    private static final String PRIMARY_CALENDAR_ID = "primary";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    public Calendar buildCalendarClient(String accessToken) {
        try {
            UserCredentials credentials = UserCredentials.newBuilder()
                    .setClientId(googleClientId)
                    .setClientSecret(googleClientSecret)
                    .setRefreshToken(accessToken)
                    .build();

            return new Calendar.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance(),
                    new HttpCredentialsAdapter(credentials)
            ).setApplicationName(APPLICATION_NAME).build();

        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Google Calendar 클라이언트 생성 실패", e);
        }
    }

    // ===== 종일 이벤트 (기존 — 하위 호환 유지) =====

    public Event createAllDayEvent(Calendar client, String title, LocalDate date) throws IOException {
        Event event = buildAllDayEvent(title, null, date, date);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event createAllDayEvent(Calendar client, String title, LocalDate startDate, LocalDate endDate)
            throws IOException {
        Event event = buildAllDayEvent(title, null, startDate, endDate);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event updateAllDayEvent(Calendar client, String eventId, String title,
                                   LocalDate startDate, LocalDate endDate) throws IOException {
        Event event = buildAllDayEvent(title, null, startDate, endDate);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    public Event updateAllDayEvent(Calendar client, String eventId, String title,
                                   LocalDate date) throws IOException {
        Event event = buildAllDayEvent(title, null, date, date);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    // ===== 종일 이벤트 (description 포함) =====

    public Event createAllDayEvent(Calendar client, String title, String description,
                                   LocalDate date) throws IOException {
        Event event = buildAllDayEvent(title, description, date, date);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event createAllDayEvent(Calendar client, String title, String description,
                                   LocalDate startDate, LocalDate endDate) throws IOException {
        Event event = buildAllDayEvent(title, description, startDate, endDate);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event updateAllDayEvent(Calendar client, String eventId, String title, String description,
                                   LocalDate date) throws IOException {
        Event event = buildAllDayEvent(title, description, date, date);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    public Event updateAllDayEvent(Calendar client, String eventId, String title, String description,
                                   LocalDate startDate, LocalDate endDate) throws IOException {
        Event event = buildAllDayEvent(title, description, startDate, endDate);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    // ===== 시간 지정 이벤트 =====

    public Event createTimedEvent(Calendar client, String title,
                                  LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        Event event = buildTimedEvent(title, null, startDateTime, endDateTime);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event createTimedEvent(Calendar client, String title, String description,
                                  LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        Event event = buildTimedEvent(title, description, startDateTime, endDateTime);
        return client.events().insert(PRIMARY_CALENDAR_ID, event).execute();
    }

    public Event updateTimedEvent(Calendar client, String eventId, String title,
                                  LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        Event event = buildTimedEvent(title, null, startDateTime, endDateTime);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    public Event updateTimedEvent(Calendar client, String eventId, String title, String description,
                                  LocalDateTime startDateTime, LocalDateTime endDateTime) throws IOException {
        Event event = buildTimedEvent(title, description, startDateTime, endDateTime);
        return client.events().update(PRIMARY_CALENDAR_ID, eventId, event).execute();
    }

    // ===== 단건 조회 / 삭제 =====

    public void deleteEvent(Calendar client, String eventId) throws IOException {
        client.events().delete(PRIMARY_CALENDAR_ID, eventId).execute();
    }

    public Event getEvent(Calendar client, String eventId) throws IOException {
        return client.events().get(PRIMARY_CALENDAR_ID, eventId).execute();
    }

    // ===== 전체 이벤트 조회 (현재 기준 ±3년, 페이지네이션 포함) =====

    public List<Event> listAllEvents(Calendar client) throws IOException {
        DateTime timeMin = new DateTime(ZonedDateTime.now(KST).minusYears(3).toInstant().toEpochMilli());
        DateTime timeMax = new DateTime(ZonedDateTime.now(KST).plusYears(3).toInstant().toEpochMilli());

        List<Event> allEvents = new ArrayList<>();
        String pageToken = null;

        do {
            Events events = client.events().list(PRIMARY_CALENDAR_ID)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .setTimeMin(timeMin)
                    .setTimeMax(timeMax)
                    .setPageToken(pageToken)
                    .execute();
            List<Event> items = events.getItems();
            if (items != null) {
                allEvents.addAll(items);
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);

        return allEvents;
    }

    // ===== import 파싱 유틸 =====

    /**
     * Google Event의 start를 LocalDate로 변환한다.
     * 종일 이벤트(date)와 시간 지정 이벤트(dateTime) 모두 처리한다.
     */
    public LocalDate parseEventStartDate(Event event) {
        EventDateTime start = event.getStart();
        if (start.getDate() != null) {
            return LocalDate.parse(start.getDate().toString());
        }
        return Instant.ofEpochMilli(start.getDateTime().getValue())
                .atZone(KST)
                .toLocalDate();
    }

    /**
     * Google Event의 end를 LocalDate로 변환한다.
     * 종일 이벤트는 endDate가 exclusive이므로 -1일 역산한다.
     */
    public LocalDate parseEventEndDate(Event event) {
        EventDateTime end = event.getEnd();
        if (end.getDate() != null) {
            return LocalDate.parse(end.getDate().toString()).minusDays(1);
        }
        return Instant.ofEpochMilli(end.getDateTime().getValue())
                .atZone(KST)
                .toLocalDate();
    }

    // ===== private builders =====

    private Event buildAllDayEvent(String title, String description, LocalDate startDate, LocalDate endDate) {
        EventDateTime start = new EventDateTime().setDate(new DateTime(startDate.toString()));
        EventDateTime end = new EventDateTime().setDate(new DateTime(endDate.plusDays(1).toString()));

        Event event = new Event()
                .setSummary(title)
                .setStart(start)
                .setEnd(end);

        if (description != null && !description.isBlank()) {
            event.setDescription(description);
        }
        return event;
    }

    private Event buildTimedEvent(String title, String description,
                                  LocalDateTime startDateTime, LocalDateTime endDateTime) {
        EventDateTime start = new EventDateTime()
                .setDateTime(new DateTime(startDateTime.atZone(KST).toInstant().toEpochMilli()))
                .setTimeZone(KST.getId());
        EventDateTime end = new EventDateTime()
                .setDateTime(new DateTime(endDateTime.atZone(KST).toInstant().toEpochMilli()))
                .setTimeZone(KST.getId());

        Event event = new Event()
                .setSummary(title)
                .setStart(start)
                .setEnd(end);

        if (description != null && !description.isBlank()) {
            event.setDescription(description);
        }
        return event;
    }
}
