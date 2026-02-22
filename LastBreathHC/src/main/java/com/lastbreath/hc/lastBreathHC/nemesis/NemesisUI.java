package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NemesisUI {
    private final LastBreathHC plugin;
    private final CaptainRegistry registry;

    private final Map<UUID, BossBar> playerBars = new HashMap<>();
    private BukkitTask uiTask;

    public NemesisUI(LastBreathHC plugin, CaptainRegistry registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    public void start() {
        stop();
        uiTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void stop() {
        if (uiTask != null) {
            uiTask.cancel();
            uiTask = null;
        }
        for (BossBar bar : playerBars.values()) {
            bar.removeAll();
        }
        playerBars.clear();
    }

    public void announceCaptainBirth(CaptainRecord record, Player victim) {
        String name = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        Bukkit.broadcastMessage("§4☠ §cA Nemesis Captain rises: §6" + name + "§7 (hunting " + victim.getName() + ")");
    }

    public void taunt(Player target, CaptainRecord record, String reason) {
        String name = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        target.sendMessage("§8[§4Nemesis§8] §6" + name + "§7: " + reason);
    }

    private void tick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            CaptainRecord nearest = nearestActiveCaptain(player);
            if (nearest == null) {
                clearBossbar(player);
                continue;
            }

            LivingEntity captainEntity = resolveCaptainEntity(nearest);
            if (captainEntity == null) {
                clearBossbar(player);
                continue;
            }

            double distance = captainEntity.getLocation().distance(player.getLocation());
            if (distance > 64) {
                clearBossbar(player);
                continue;
            }

            BossBar bar = playerBars.computeIfAbsent(player.getUniqueId(), ignored ->
                    Bukkit.createBossBar("Nemesis", BarColor.RED, BarStyle.SEGMENTED_10));
            String name = nearest.naming() == null ? "Nemesis Captain" : nearest.naming().displayName();
            bar.setTitle("§4Nemesis: §6" + name + " §7Lv." + nearest.progression().level());
            double progress = captainEntity.getMaxHealth() <= 0 ? 1.0 : captainEntity.getHealth() / captainEntity.getMaxHealth();
            bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            if (!bar.getPlayers().contains(player)) {
                bar.addPlayer(player);
            }

            player.sendActionBar(Component.text("§cNemesis nearby: §6" + name + " §7(" + (int) distance + "m)"));
        }
    }

    private CaptainRecord nearestActiveCaptain(Player player) {
        return registry.getAll().stream()
                .filter(record -> record.state() != null && record.state().active())
                .filter(record -> {
                    LivingEntity entity = resolveCaptainEntity(record);
                    return entity != null && entity.getWorld().equals(player.getWorld());
                })
                .min(Comparator.comparingDouble(record -> {
                    LivingEntity entity = resolveCaptainEntity(record);
                    return entity == null ? Double.MAX_VALUE : entity.getLocation().distanceSquared(player.getLocation());
                }))
                .orElse(null);
    }

    private LivingEntity resolveCaptainEntity(CaptainRecord record) {
        if (record.identity() == null || record.identity().spawnEntityUuid() == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(record.identity().spawnEntityUuid());
        return entity instanceof LivingEntity living && living.isValid() ? living : null;
    }

    private void clearBossbar(Player player) {
        BossBar bar = playerBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.removeAll();
        }
    }
}
