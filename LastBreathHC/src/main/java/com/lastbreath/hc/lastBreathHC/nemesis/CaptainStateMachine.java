package com.lastbreath.hc.lastBreathHC.nemesis;

public class CaptainStateMachine {

    public CaptainRecord.State onCreate(long nowEpochMs) {
        CaptainRecord.State candidate = new CaptainRecord.State(CaptainState.CANDIDATE, 0L, nowEpochMs, null);
        return new CaptainRecord.State(CaptainState.DORMANT, candidate.cooldownUntilEpochMs(), candidate.lastSeenEpochMs(), null);
    }

    public CaptainRecord.State onSpawn(long nowEpochMs) {
        return new CaptainRecord.State(CaptainState.ACTIVE, 0L, nowEpochMs, null);
    }

    public CaptainRecord.State onEscapeOrDespawn(long nowEpochMs, long cooldownUntilEpochMs) {
        return new CaptainRecord.State(CaptainState.COOLDOWN, cooldownUntilEpochMs, nowEpochMs, null);
    }

    public CaptainRecord.State onCooldownElapsed(long nowEpochMs) {
        return new CaptainRecord.State(CaptainState.DORMANT, 0L, nowEpochMs, null);
    }

    public CaptainRecord.State onKilled(long nowEpochMs) {
        return new CaptainRecord.State(CaptainState.DEAD, 0L, nowEpochMs, null);
    }

    public CaptainRecord.State onRetire(long nowEpochMs) {
        return new CaptainRecord.State(CaptainState.RETIRED, 0L, nowEpochMs, null);
    }
}
