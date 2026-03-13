package com.lastbreath.hc.lastBreathHC.holiday;

import java.time.LocalDate;

public record HolidayWeekEvent(HolidayType holidayType, LocalDate holidayDate, LocalDate weekStart, LocalDate weekEnd) {

    public boolean isActive(LocalDate date) {
        return !date.isBefore(weekStart) && !date.isAfter(weekEnd);
    }
}
