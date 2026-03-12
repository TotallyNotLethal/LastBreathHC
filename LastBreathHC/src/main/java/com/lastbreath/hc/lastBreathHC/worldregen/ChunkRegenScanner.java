package com.lastbreath.hc.lastBreathHC.worldregen;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;

import java.util.EnumSet;
import java.util.Set;

public final class ChunkRegenScanner {

    private static final Set<Material> SUSPICIOUS_TILE_MATERIALS = EnumSet.of(
            Material.CHEST,
            Material.BARREL,
            Material.FURNACE,
            Material.BLAST_FURNACE,
            Material.SMOKER,
            Material.HOPPER,
            Material.DROPPER,
            Material.DISPENSER,
            Material.BREWING_STAND
    );

    private final Chunk chunk;
    private final int minY;
    private final int maxY;
    private int x;
    private int y;
    private int z;
    private boolean started;

    public ChunkRegenScanner(Chunk chunk) {
        this.chunk = chunk;
        World world = chunk.getWorld();
        this.minY = world.getMinHeight();
        this.maxY = world.getMaxHeight() - 1;
        this.x = 0;
        this.y = minY;
        this.z = 0;
    }

    public ScanResult scan(int maxBlocks) {
        if (!started) {
            started = true;
            if (chunk.getTileEntities().length > 0) {
                return ScanResult.playerModified("Tile entities present in chunk.");
            }
        }

        int processed = 0;
        while (processed < maxBlocks) {
            if (y > maxY) {
                return ScanResult.safe(processed);
            }

            Block block = chunk.getBlock(x, y, z);
            Material material = block.getType();
            if (isSuspicious(material)) {
                return ScanResult.playerModified("Detected player-ish block: " + material.name());
            }

            BlockState state = block.getState(false);
            if (state != null && SUSPICIOUS_TILE_MATERIALS.contains(state.getType())) {
                return ScanResult.playerModified("Detected tile entity block: " + state.getType().name());
            }

            processed++;
            advance();
        }

        return ScanResult.inProgress(processed);
    }

    private void advance() {
        x++;
        if (x >= 16) {
            x = 0;
            z++;
            if (z >= 16) {
                z = 0;
                y++;
            }
        }
    }

    private boolean isSuspicious(Material material) {
        if (material == Material.COBBLESTONE
                || material == Material.TORCH
                || material == Material.WALL_TORCH
                || material == Material.CRAFTING_TABLE
                || material == Material.GLASS
                || material == Material.GLASS_PANE) {
            return true;
        }

        String name = material.name();
        return name.endsWith("_PLANKS")
                || name.endsWith("_STAIRS")
                || name.endsWith("_SLAB")
                || name.endsWith("_BED")
                || name.endsWith("_DOOR");
    }

    public record ScanResult(State state, int blocksProcessed, String reason) {
        public static ScanResult inProgress(int blocksProcessed) {
            return new ScanResult(State.IN_PROGRESS, blocksProcessed, "");
        }

        public static ScanResult safe(int blocksProcessed) {
            return new ScanResult(State.SAFE, blocksProcessed, "");
        }

        public static ScanResult playerModified(String reason) {
            return new ScanResult(State.PLAYER_MODIFIED, 0, reason);
        }
    }

    public enum State {
        IN_PROGRESS,
        SAFE,
        PLAYER_MODIFIED
    }
}
