package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.List;

public record HolidayEventDefinition(
        String eventName,
        String objective,
        HolidayEventZone zone,
        List<HolidayTaskDefinition> tasks,
        List<HolidayRewardDefinition> rewards
) {
}
