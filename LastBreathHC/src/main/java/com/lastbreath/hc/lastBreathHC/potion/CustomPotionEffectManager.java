package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CustomPotionEffectManager implements Listener {

    private static final int MAX_DURATION_TICKS = 20 * 60 * 20;
    private static final int REDSTONE_BONUS_TICKS = 2 * 60 * 20;

    private final LastBreathHC plugin;
    private final PotionDefinitionRegistry definitionRegistry;
    private final CustomPotionEffectRegistry effectRegistry;
    private final NamespacedKey customIdKey;
    private final NamespacedKey redstoneKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Map<String, Long>> activeEffects = new HashMap<>();

    public CustomPotionEffectManager(LastBreathHC plugin,
                                     PotionDefinitionRegistry definitionRegistry,
                                     CustomPotionEffectRegistry effectRegistry) {
        this.plugin = plugin;
        this.definitionRegistry = definitionRegistry;
        this.effectRegistry = effectRegistry;
        this.customIdKey = new NamespacedKey(plugin, "potion_custom_id");
        this.redstoneKey = new NamespacedKey(plugin, "potion_redstone_apps");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionDrink(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();
        if (item.getItemMeta() instanceof PotionMeta meta) {
            PersistentDataContainer container = meta.getPersistentDataContainer();
            String customId = container.get(customIdKey, PersistentDataType.STRING);
            if (customId == null) {
                return;
            }
            HardcorePotionDefinition definition = definitionRegistry.getById(customId);
            if (definition == null) {
                return;
            }
            int redstoneApps = container.getOrDefault(redstoneKey, PersistentDataType.INTEGER, 0);
            applyCustomEffects(event.getPlayer(), definition.customEffects(), definition, redstoneApps);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        cooldowns.remove(playerId);
        activeEffects.remove(playerId);
    }

    private void applyCustomEffects(Player player,
                                    List<HardcorePotionDefinition.CustomEffectDefinition> customEffects,
                                    HardcorePotionDefinition definition,
                                    int redstoneApps) {
        if (customEffects.isEmpty()) {
            return;
        }
        int delayTicks = calculateAfterEffectDelay(definition, redstoneApps);
        for (HardcorePotionDefinition.CustomEffectDefinition customEffect : customEffects) {
            int durationTicks = extendDuration(customEffect.durationTicks(), redstoneApps);
            if (customEffect.trigger() == EffectTrigger.AFTER_EFFECT && delayTicks > 0) {
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        activateCustomEffect(player, customEffect, durationTicks);
                    }
                }.runTaskLater(plugin, delayTicks);
            } else {
                activateCustomEffect(player, customEffect, durationTicks);
            }
        }
    }

    private void activateCustomEffect(Player player,
                                      HardcorePotionDefinition.CustomEffectDefinition customEffect,
                                      int durationTicks) {
        String effectId = customEffect.id();
        if (effectRegistry.getById(effectId) == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> playerCooldowns = cooldowns.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        long lastUsed = playerCooldowns.getOrDefault(effectId, 0L);
        if (customEffect.cooldownTicks() > 0) {
            long cooldownMillis = ticksToMillis(customEffect.cooldownTicks());
            if (now - lastUsed < cooldownMillis) {
                return;
            }
            playerCooldowns.put(effectId, now);
        }

        Map<String, Long> playerEffects = activeEffects.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        long expiresAt = now + ticksToMillis(durationTicks);
        playerEffects.put(effectId, expiresAt);

        if (durationTicks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<String, Long> effects = activeEffects.get(player.getUniqueId());
                    if (effects != null && effects.getOrDefault(effectId, 0L) <= System.currentTimeMillis()) {
                        effects.remove(effectId);
                    }
                }
            }.runTaskLater(plugin, durationTicks);
        }
    }

    private int calculateAfterEffectDelay(HardcorePotionDefinition definition, int redstoneApps) {
        int maxDuration = 0;
        for (HardcorePotionDefinition.EffectDefinition effect : definition.baseEffects()) {
            maxDuration = Math.max(maxDuration, extendDuration(effect.durationTicks(), redstoneApps));
        }
        for (HardcorePotionDefinition.EffectDefinition effect : definition.drawbacks()) {
            maxDuration = Math.max(maxDuration, extendDuration(effect.durationTicks(), redstoneApps));
        }
        return maxDuration;
    }

    private int extendDuration(int durationTicks, int redstoneApps) {
        if (durationTicks <= 0 || redstoneApps <= 0) {
            return durationTicks;
        }
        long extended = (long) durationTicks + ((long) redstoneApps * REDSTONE_BONUS_TICKS);
        return (int) Math.min(extended, MAX_DURATION_TICKS);
    }

    private long ticksToMillis(int ticks) {
        return (long) ticks * 50L;
    }

    public boolean hasActiveEffect(Player player, String effectId) {
        Map<String, Long> effects = activeEffects.get(player.getUniqueId());
        if (effects == null) {
            return false;
        }
        long expiresAt = effects.getOrDefault(effectId, 0L);
        return expiresAt > System.currentTimeMillis();
    }

    public List<String> getActiveEffectIds(Player player) {
        Map<String, Long> effects = activeEffects.get(player.getUniqueId());
        if (effects == null) {
            return List.of();
        }
        long now = System.currentTimeMillis();
        List<String> active = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : effects.entrySet()) {
            if (entry.getValue() > now) {
                active.add(entry.getKey());
            }
        }
        return active;
    }

    public Map<String, Long> getActiveEffectRemainingMillis(Player player) {
        Map<String, Long> effects = activeEffects.get(player.getUniqueId());
        if (effects == null) {
            return Map.of();
        }
        long now = System.currentTimeMillis();
        Map<String, Long> remaining = new HashMap<>();
        for (Map.Entry<String, Long> entry : effects.entrySet()) {
            long remainingMillis = entry.getValue() - now;
            if (remainingMillis > 0) {
                remaining.put(entry.getKey(), remainingMillis);
            }
        }
        return remaining;
    }

    public boolean activateEffect(Player player, String effectId, int durationTicks) {
        if (effectRegistry.getById(effectId) == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> playerEffects = activeEffects.computeIfAbsent(player.getUniqueId(), key -> new HashMap<>());
        long expiresAt = now + ticksToMillis(durationTicks);
        playerEffects.put(effectId, expiresAt);
        if (durationTicks > 0) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    Map<String, Long> effects = activeEffects.get(player.getUniqueId());
                    if (effects != null && effects.getOrDefault(effectId, 0L) <= System.currentTimeMillis()) {
                        effects.remove(effectId);
                    }
                }
            }.runTaskLater(plugin, durationTicks);
        }
        return true;
    }
}
