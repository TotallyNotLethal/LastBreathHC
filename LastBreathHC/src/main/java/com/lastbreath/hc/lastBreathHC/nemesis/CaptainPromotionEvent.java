package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.UUID;

public record CaptainPromotionEvent(
        UUID captainId,
        double previousPromotionScore,
        double currentPromotionScore,
        double crossedThreshold,
        long occurredAtEpochMillis
) {
}
