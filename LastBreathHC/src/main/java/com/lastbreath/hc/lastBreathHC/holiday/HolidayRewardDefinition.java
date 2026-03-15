package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.concurrent.ThreadLocalRandom;

public record HolidayRewardDefinition(HolidayRewardType type, String target, int amount, String command, double chance) {

    public boolean shouldGrant() {
        return chance >= 1.0D || ThreadLocalRandom.current().nextDouble() <= chance;
    }
}
