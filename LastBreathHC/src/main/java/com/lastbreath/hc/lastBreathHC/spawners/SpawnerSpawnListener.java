package com.lastbreath.hc.lastBreathHC.spawners;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.NamespacedKey;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class SpawnerSpawnListener implements Listener {

    private final NamespacedKey playerPlacedSpawnerKey;

    public SpawnerSpawnListener(LastBreathHC plugin) {
        this.playerPlacedSpawnerKey = new NamespacedKey(plugin, SpawnerTags.PLAYER_PLACED_SPAWNER_KEY);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        CreatureSpawner spawner = event.getSpawner();
        PersistentDataContainer container = spawner.getPersistentDataContainer();
        if (!container.has(playerPlacedSpawnerKey, PersistentDataType.BYTE)) {
            return;
        }

        Entity spawned = event.getEntity();
        if (!(spawned instanceof LivingEntity livingEntity)) {
            return;
        }

        livingEntity.addScoreboardTag(SpawnerTags.PLAYER_SPAWNER_MOB_TAG);
    }
}
