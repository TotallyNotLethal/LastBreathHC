package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NemesisRewardService implements Listener {
    private final LastBreathHC plugin;
    private final CaptainEntityBinder binder;
    private final CaptainRegistry registry;
    private final Map<UUID, Long> playerCooldown = new HashMap<>();

    public NemesisRewardService(LastBreathHC plugin, CaptainEntityBinder binder, CaptainRegistry registry) {
        this.plugin = plugin;
        this.binder = binder;
        this.registry = registry;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCaptainDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        CaptainRecord record = binder.resolveCaptainRecord(living).orElse(null);
        if (record == null) {
            return;
        }

        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        long cooldown = Math.max(1_000L, plugin.getConfig().getLong("nemesis.rewards.playerCooldownMs", 120_000L));
        long now = System.currentTimeMillis();
        if (now < playerCooldown.getOrDefault(killer.getUniqueId(), 0L)) {
            return;
        }
        playerCooldown.put(killer.getUniqueId(), now + cooldown);

        event.getDrops().add(buildNemesisToken());
        double bonusChance = plugin.getConfig().getDouble("nemesis.rewards.bonusChance", 0.1);
        if (Math.random() <= bonusChance) {
            event.getDrops().add(new ItemStack(Material.DIAMOND, 1));
        }

        Map<String, Long> counters = new HashMap<>(record.telemetry().counters());
        counters.put("rewardsGranted", counters.getOrDefault("rewardsGranted", 0L) + 1L);
        CaptainRecord.Telemetry telemetry = new CaptainRecord.Telemetry(now, now, record.telemetry().encounters(), counters);
        registry.upsert(new CaptainRecord(record.identity(), record.origin(), record.victims(), record.nemesisScores(), record.progression(), record.naming(), record.traits(), record.minionPack(), record.state(), telemetry));
    }

    private ItemStack buildNemesisToken() {
        ItemStack token = new ItemStack(Material.NETHER_STAR, 1);
        ItemMeta meta = token.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§5Nemesis Token");
            token.setItemMeta(meta);
        }
        return token;
    }
}
