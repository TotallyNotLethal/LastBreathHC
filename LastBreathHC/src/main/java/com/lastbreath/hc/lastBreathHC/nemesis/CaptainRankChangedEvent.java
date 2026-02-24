package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.UUID;

public record CaptainRankChangedEvent(
        UUID captainId,
        Rank previousRank,
        Rank currentRank,
        long occurredAtEpochMillis
) {
}
