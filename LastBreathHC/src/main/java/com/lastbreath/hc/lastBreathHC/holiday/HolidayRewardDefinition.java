package com.lastbreath.hc.lastBreathHC.holiday;

import java.util.concurrent.ThreadLocalRandom;

public record HolidayRewardDefinition(HolidayRewardType type,
                                      String target,
                                      int amount,
                                      String command,
                                      double chance,
                                      String pool,
                                      int weight) {

    public boolean shouldGrant() {
        return chance >= 1.0D || ThreadLocalRandom.current().nextDouble() <= chance;
    }

    public boolean isPooledCustomItem() {
        return type == HolidayRewardType.CUSTOM_ITEM && !pool.isBlank();
    }
}
