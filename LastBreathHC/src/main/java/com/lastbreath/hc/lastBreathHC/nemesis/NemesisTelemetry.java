package com.lastbreath.hc.lastBreathHC.nemesis;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class NemesisTelemetry {
    private NemesisTelemetry() {
    }

    public static CaptainRecord incrementCounter(CaptainRecord record, String key, long amount) {
        if (record == null || key == null || key.isBlank() || amount == 0L) {
            return record;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> counters = new HashMap<>(record.telemetry().counters());
        counters.put(key, counters.getOrDefault(key, 0L) + amount);
        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(now, now, record.telemetry().encounters(), counters);
        return new CaptainRecord(
                record.identity(),
                record.origin(),
                record.victims(),
                record.nemesisScores(),
                record.progression(),
                record.naming(),
                record.traits(),
                record.minionPack(),
                record.state(),
                telemetry,
                record.political(),
                record.social(),
                record.relationships(),
                record.memory(),
                record.persona()
        );
    }

    public static CaptainRecord withSocial(CaptainRecord record, CaptainRecord.Social social) {
        return new CaptainRecord(
                record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(),
                record.naming(), record.traits(), record.minionPack(), record.state(), record.telemetry(),
                record.political(), Optional.ofNullable(social), record.relationships(), record.memory(), record.persona()
        );
    }
}
