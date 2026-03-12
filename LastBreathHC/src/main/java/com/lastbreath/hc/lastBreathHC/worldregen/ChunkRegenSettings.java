package com.lastbreath.hc.lastBreathHC.worldregen;

import org.bukkit.configuration.file.FileConfiguration;

public record ChunkRegenSettings(
        boolean enabled,
        int chunksPerTick,
        int scanBlocksPerTick,
        boolean skipLoaded,
        int scanRadius,
        boolean detectPlayerBlocks
) {

    public static ChunkRegenSettings fromConfig(FileConfiguration config) {
        boolean enabled = config.getBoolean("world-regen.enabled", false);
        int chunksPerTick = Math.max(1, config.getInt("world-regen.chunks-per-tick", 4));
        int scanBlocksPerTick = Math.max(512, config.getInt("world-regen.scan-blocks-per-tick", 5000));
        boolean skipLoaded = config.getBoolean("world-regen.skip-loaded", true);
        int scanRadius = Math.max(16, config.getInt("world-regen.scan-radius", 20000));
        boolean detectPlayerBlocks = config.getBoolean("world-regen.detect-player-blocks", true);
        return new ChunkRegenSettings(enabled, chunksPerTick, scanBlocksPerTick, skipLoaded, scanRadius, detectPlayerBlocks);
    }
}
