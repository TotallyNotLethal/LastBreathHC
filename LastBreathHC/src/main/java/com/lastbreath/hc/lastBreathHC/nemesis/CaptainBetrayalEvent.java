package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.UUID;

public record CaptainBetrayalEvent(
        UUID attackerCaptainId,
        UUID victimCaptainId,
        String reason,
        long occurredAtEpochMillis
) {
}
