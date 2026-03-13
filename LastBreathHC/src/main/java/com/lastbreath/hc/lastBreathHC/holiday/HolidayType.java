package com.lastbreath.hc.lastBreathHC.holiday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;

public enum HolidayType {
    NEW_YEAR("New Year", 1, "Midnight Meteor Relay", "Collect 8 Spark Fragments from shooting stars and light the Sky Beacon before dawn."),
    VALENTINES("Valentine's Day", 2, "Cupid's Lost Letters", "Recover scattered love letters from rose groves while Heartbreak Wraiths try to steal them."),
    ST_PATRICKS("St. Patrick's Day", 3, "Emerald Gremlin Hunt", "Track prankster gremlins and reclaim stolen emerald charms from hidden burrows."),
    EASTER("Easter", 4, "Colossal Bunny Rampage", "Defeat giant war bunnies and recover painted relic eggs to assemble the Spring Totem."),
    INDEPENDENCE_DAY("Independence Day", 7, "Powder Keg Convoy", "Escort unstable firework carts and ignite signal towers before raiders blow them up."),
    HALLOWEEN("Halloween", 10, "Lanterns of the Hollow", "Harvest cursed lantern cores at night and seal haunted rifts before sunrise."),
    THANKSGIVING("Thanksgiving", 11, "Harvest Guardian Feast", "Gather sacred crops while defending village ovens from ravenous scarecrow brutes."),
    CHRISTMAS("Christmas", 12, "Snowman Salvage Operation", "Find missing clockwork snowmen and collect toy parts to rebuild Santa's workshop golems.");

    private final String displayName;
    private final int monthValue;
    private final String eventName;
    private final String objective;

    HolidayType(String displayName, int monthValue, String eventName, String objective) {
        this.displayName = displayName;
        this.monthValue = monthValue;
        this.eventName = eventName;
        this.objective = objective;
    }

    public String displayName() {
        return displayName;
    }

    public String eventName() {
        return eventName;
    }

    public String objective() {
        return objective;
    }

    public LocalDate holidayDate(int year) {
        return switch (this) {
            case NEW_YEAR -> LocalDate.of(year, Month.JANUARY, 1);
            case VALENTINES -> LocalDate.of(year, Month.FEBRUARY, 14);
            case ST_PATRICKS -> LocalDate.of(year, Month.MARCH, 17);
            case EASTER -> easterSunday(year);
            case INDEPENDENCE_DAY -> LocalDate.of(year, Month.JULY, 4);
            case HALLOWEEN -> LocalDate.of(year, Month.OCTOBER, 31);
            case THANKSGIVING -> LocalDate.of(year, Month.NOVEMBER, 1)
                    .with(TemporalAdjusters.dayOfWeekInMonth(4, DayOfWeek.THURSDAY));
            case CHRISTMAS -> LocalDate.of(year, Month.DECEMBER, 25);
        };
    }

    public int monthValue() {
        return monthValue;
    }

    private static LocalDate easterSunday(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(year, month, day);
    }
}
