package com.lastbreath.hc.lastBreathHC.holiday;

public enum HolidayTaskType {
    KILL_ENTITY,
    BREAK_BLOCK,
    COLLECT_ITEM;

    public static HolidayTaskType fromString(String raw) {
        return valueOf(raw.trim().toUpperCase());
    }
}
