package com.lastbreath.hc.lastBreathHC.holiday;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class HolidayGameplayManager implements Listener {

    private final JavaPlugin plugin;
    private final HolidayEventManager holidayEventManager;
    private final HolidayEventConfig holidayEventConfig;
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Set<UUID> completed = new HashSet<>();
    private HolidayType activeHoliday;
    private BukkitTask stateTask;

    public HolidayGameplayManager(JavaPlugin plugin, HolidayEventManager holidayEventManager, HolidayEventConfig holidayEventConfig) {
        this.plugin = plugin;
        this.holidayEventManager = holidayEventManager;
        this.holidayEventConfig = holidayEventConfig;
    }

    public void start() {
        updateHolidayState(true);
        stateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateHolidayState(false), 20L * 60L, 20L * 60L * 10L);
    }

    public void stop() {
        if (stateTask != null) {
            stateTask.cancel();
            stateTask = null;
        }
    }

    public Optional<HolidayEventDefinition> getActiveDefinition() {
        if (activeHoliday == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(holidayEventConfig.definitionFor(activeHoliday));
    }

    public String progressLine(Player player) {
        if (activeHoliday == null) {
            return "No active holiday gameplay event.";
        }
        HolidayEventDefinition definition = holidayEventConfig.definitionFor(activeHoliday);
        if (definition == null) {
            return "No configured gameplay event for " + activeHoliday.displayName() + ".";
        }
        Map<String, Integer> playerProgress = progress.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        StringBuilder builder = new StringBuilder("Progress for ").append(activeHoliday.displayName()).append(": ");
        for (HolidayTaskDefinition task : definition.tasks()) {
            int current = Math.min(task.amount(), playerProgress.getOrDefault(task.progressKey(), 0));
            builder.append(task.type().name()).append(" ").append(task.target()).append(" ").append(current).append("/").append(task.amount()).append("; ");
        }
        return builder.toString();
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }
        applyProgress(killer, HolidayTaskType.KILL_ENTITY, event.getEntityType().name(), 1, killer.getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        applyProgress(event.getPlayer(), HolidayTaskType.BREAK_BLOCK, event.getBlock().getType().name(), 1, event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(PlayerAttemptPickupItemEvent event) {
        applyProgress(event.getPlayer(), HolidayTaskType.COLLECT_ITEM, event.getItem().getItemStack().getType().name(), event.getItem().getItemStack().getAmount(), event.getPlayer().getLocation());
    }

    private void applyProgress(Player player, HolidayTaskType taskType, String target, int delta, org.bukkit.Location location) {
        if (activeHoliday == null || completed.contains(player.getUniqueId())) {
            return;
        }
        HolidayEventDefinition definition = holidayEventConfig.definitionFor(activeHoliday);
        if (definition == null || !definition.zone().contains(location)) {
            return;
        }

        Map<String, Integer> playerProgress = progress.computeIfAbsent(player.getUniqueId(), id -> new HashMap<>());
        boolean changed = false;
        for (HolidayTaskDefinition task : definition.tasks()) {
            if (task.type() != taskType || !task.target().equalsIgnoreCase(target)) {
                continue;
            }
            String key = task.progressKey();
            int next = Math.min(task.amount(), playerProgress.getOrDefault(key, 0) + delta);
            playerProgress.put(key, next);
            changed = true;
        }

        if (!changed) {
            return;
        }

        if (isComplete(playerProgress, definition)) {
            completed.add(player.getUniqueId());
            rewardPlayer(player, definition);
        }
    }

    private boolean isComplete(Map<String, Integer> playerProgress, HolidayEventDefinition definition) {
        for (HolidayTaskDefinition task : definition.tasks()) {
            if (playerProgress.getOrDefault(task.progressKey(), 0) < task.amount()) {
                return false;
            }
        }
        return true;
    }

    private void rewardPlayer(Player player, HolidayEventDefinition definition) {
        player.sendMessage(Component.text("Holiday objective complete! Rewards granted.", NamedTextColor.GOLD));
        for (HolidayRewardDefinition reward : definition.rewards()) {
            switch (reward.type()) {
                case ITEM -> {
                    Material material = Material.matchMaterial(reward.target());
                    if (material == null) {
                        continue;
                    }
                    Map<Integer, ItemStack> overflow = player.getInventory().addItem(new ItemStack(material, reward.amount()));
                    overflow.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
                }
                case XP -> player.giveExp(reward.amount());
                case COMMAND -> {
                    String command = reward.command()
                            .replace("%player%", player.getName())
                            .replace("%holiday%", activeHoliday.name().toLowerCase());
                    if (!command.isBlank()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
                    }
                }
            }
        }
    }

    private void updateHolidayState(boolean startup) {
        HolidayType previous = activeHoliday;
        activeHoliday = holidayEventManager.getHolidayEventForDate(LocalDate.now())
                .map(HolidayWeekEvent::holidayType)
                .orElse(null);

        if (previous == activeHoliday) {
            return;
        }

        progress.clear();
        completed.clear();

        if (activeHoliday != null) {
            HolidayEventDefinition definition = holidayEventConfig.definitionFor(activeHoliday);
            if (definition != null && !startup) {
                Bukkit.broadcast(Component.text("Holiday week started: " + activeHoliday.displayName() + " - " + definition.eventName(), NamedTextColor.LIGHT_PURPLE));
            }
        } else if (previous != null && !startup) {
            Bukkit.broadcast(Component.text("Holiday week ended: " + previous.displayName(), NamedTextColor.GRAY));
        }
    }
}
