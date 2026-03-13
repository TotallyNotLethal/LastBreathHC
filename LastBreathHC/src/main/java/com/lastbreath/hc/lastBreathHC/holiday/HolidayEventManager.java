package com.lastbreath.hc.lastBreathHC.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class HolidayEventManager {

    public Optional<HolidayWeekEvent> getActiveHolidayEvent() {
        return getHolidayEventForDate(LocalDate.now());
    }

    public Optional<HolidayWeekEvent> getHolidayEventForDate(LocalDate date) {
        return Arrays.stream(HolidayType.values())
                .map(holiday -> toWeekEvent(holiday, date.getYear()))
                .filter(event -> event.isActive(date))
                .findFirst();
    }

    public List<HolidayWeekEvent> upcomingEvents(int count) {
        LocalDate today = LocalDate.now();
        int currentYear = Year.now().getValue();

        return Arrays.stream(HolidayType.values())
                .flatMap(holiday -> Arrays.stream(new int[]{currentYear, currentYear + 1})
                        .mapToObj(year -> toWeekEvent(holiday, year)))
                .filter(event -> !event.weekEnd().isBefore(today))
                .sorted(Comparator.comparing(HolidayWeekEvent::weekStart))
                .limit(count)
                .toList();
    }

    private HolidayWeekEvent toWeekEvent(HolidayType holiday, int year) {
        LocalDate holidayDate = holiday.holidayDate(year);
        LocalDate weekStart = holidayDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = holidayDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        return new HolidayWeekEvent(holiday, holidayDate, weekStart, weekEnd);
    }
}
