package com.ssafy.springbootbe.domain.calendar.service;

import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarExportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarImportResponse;
import com.ssafy.springbootbe.domain.calendar.dto.response.CalendarResponse;

public interface CalendarService {

    CalendarResponse getCalendar(Long userId, int year, int month);

    CalendarExportResponse export(Long userId);

    CalendarImportResponse importFromGoogle(Long userId);
}
