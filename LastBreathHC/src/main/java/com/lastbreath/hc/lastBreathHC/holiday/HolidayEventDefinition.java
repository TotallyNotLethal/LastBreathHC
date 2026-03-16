package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.List;

public record HolidayEventDefinition(
        String eventName,
        String objective,
        boolean restrictCollectionToZone,
        HolidayEventZone zone,
        List<HolidayTaskDefinition> tasks,
        List<HolidayRewardDefinition> rewards
) {
}
