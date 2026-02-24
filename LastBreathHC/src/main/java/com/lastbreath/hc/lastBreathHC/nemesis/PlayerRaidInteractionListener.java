package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public final class PlayerRaidInteractionListener implements Listener {
    private final StructureEventOrchestrator orchestrator;

    public PlayerRaidInteractionListener(StructureEventOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @EventHandler(ignoreCancelled = true)
    public void onRaidTargetDestroyed(BlockBreakEvent event) {
        Material type = event.getBlock().getType();
        PlayerRaidInteractionEvent.RaidTargetType targetType = resolve(type);
        if (targetType == null) {
            return;
        }
        orchestrator.onRaidInteraction(new PlayerRaidInteractionEvent(
                event.getPlayer().getUniqueId(),
                event.getPlayer().getName(),
                targetType,
                event.getBlock().getBiome().name(),
                event.getBlock().getLocation(),
                System.currentTimeMillis()
        ));
    }

    private PlayerRaidInteractionEvent.RaidTargetType resolve(Material material) {
        if (material.name().endsWith("_BANNER")) {
            return PlayerRaidInteractionEvent.RaidTargetType.BANNER;
        }
        if (material == Material.LODESTONE) {
            return PlayerRaidInteractionEvent.RaidTargetType.THRONE;
        }
        if (material == Material.RESPAWN_ANCHOR) {
            return PlayerRaidInteractionEvent.RaidTargetType.HEART;
        }
        return null;
    }
}
