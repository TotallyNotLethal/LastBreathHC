package com.lastbreath.hc.lastBreathHC.fakeplayer.platform;

import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;

import java.util.UUID;

public class NoOpFakePlayerPlatformAdapter implements FakePlayerPlatformAdapter {
    @Override
    public void spawnFakeTabEntry(FakePlayerRecord record) {
        // Unsupported server version; visual simulation is disabled.
    }

    @Override
    public void despawnFakeTabEntry(UUID uuid) {
        // Unsupported server version; visual simulation is disabled.
    }

    @Override
    public void updateDisplayState(FakePlayerRecord record) {
        // Unsupported server version; visual simulation is disabled.
    }
}
