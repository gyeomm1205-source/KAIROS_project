package com.ssafy.springbootbe.domain.calendar.service;

import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;

public interface CalendarService {

    CalendarResponse getCalendar(Long userId, int year, int month);
}
