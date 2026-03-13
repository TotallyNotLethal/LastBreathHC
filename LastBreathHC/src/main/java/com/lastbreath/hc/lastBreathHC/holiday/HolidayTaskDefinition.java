package com.lastbreath.hc.lastBreathHC.holiday;

public record HolidayTaskDefinition(HolidayTaskType type, String target, int amount) {

    public String progressKey() {
        return type.name() + ":" + target.toUpperCase();
    }
}
