package com.lastbreath.hc.lastBreathHC.holiday;

public enum HolidayRewardType {
    ITEM,
    XP,
    COMMAND;

    public static HolidayRewardType fromString(String raw) {
        return valueOf(raw.trim().toUpperCase());
    }
}
