package com.lastbreath.hc.lastBreathHC.holiday;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
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
    private final File stateFile;
    private final Map<UUID, Map<String, Integer>> progress = new HashMap<>();
    private final Set<UUID> completed = new HashSet<>();
    private HolidayType activeHoliday;
    private BukkitTask stateTask;
    private BukkitTask autosaveTask;
    private boolean dirty;

    public HolidayGameplayManager(JavaPlugin plugin, HolidayEventManager holidayEventManager, HolidayEventConfig holidayEventConfig) {
        this.plugin = plugin;
        this.holidayEventManager = holidayEventManager;
        this.holidayEventConfig = holidayEventConfig;
        this.stateFile = new File(plugin.getDataFolder(), "holiday-gameplay-state.yml");
    }

    public void start() {
        updateHolidayState(true);
        loadPersistedStateForActiveHoliday();
        stateTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> updateHolidayState(false), 20L * 60L, 20L * 60L * 10L);
        autosaveTask = Bukkit.getScheduler().runTaskTimer(plugin, this::savePersistedStateIfDirty, 20L * 60L * 3L, 20L * 60L * 3L);
    }

    public void stop() {
        if (stateTask != null) {
            stateTask.cancel();
            stateTask = null;
        }
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
        savePersistedState();
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
            dirty = true;
            rewardPlayer(player, definition);
        }

        if (changed) {
            dirty = true;
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
                    giveOrDrop(player, new ItemStack(material, reward.amount()));
                }
                case CUSTOM_ITEM -> {
                    Optional<ItemStack> customReward = HolidayRewardItemResolver.resolve(reward.target(), reward.amount());
                    if (customReward.isEmpty()) {
                        plugin.getLogger().warning("Skipping unknown holiday custom reward item id: " + reward.target());
                        continue;
                    }
                    giveOrDrop(player, customReward.get());
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


    private void giveOrDrop(Player player, ItemStack item) {
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        overflow.values().forEach(overflowItem -> player.getWorld().dropItemNaturally(player.getLocation(), overflowItem));
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
        dirty = true;

        if (!startup) {
            clearPersistedState();
        }

        if (activeHoliday != null) {
            HolidayEventDefinition definition = holidayEventConfig.definitionFor(activeHoliday);
            if (definition != null && !startup) {
                Bukkit.broadcast(Component.text("Holiday week started: " + activeHoliday.displayName() + " - " + definition.eventName(), NamedTextColor.LIGHT_PURPLE));
            }
        } else if (previous != null && !startup) {
            Bukkit.broadcast(Component.text("Holiday week ended: " + previous.displayName(), NamedTextColor.GRAY));
        }
    }

    private void loadPersistedStateForActiveHoliday() {
        if (activeHoliday == null || !stateFile.exists()) {
            return;
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(stateFile);
        ConfigurationSection holidaySection = yaml.getConfigurationSection("holidays." + activeHoliday.name());
        if (holidaySection == null) {
            return;
        }

        ConfigurationSection playersSection = holidaySection.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        for (String uuidKey : playersSection.getKeys(false)) {
            UUID playerId;
            try {
                playerId = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            ConfigurationSection playerSection = playersSection.getConfigurationSection(uuidKey);
            if (playerSection == null) {
                continue;
            }

            Map<String, Integer> playerProgress = new HashMap<>();
            ConfigurationSection progressSection = playerSection.getConfigurationSection("progress");
            if (progressSection != null) {
                for (String key : progressSection.getKeys(false)) {
                    playerProgress.put(key, Math.max(0, progressSection.getInt(key, 0)));
                }
            }

            if (!playerProgress.isEmpty()) {
                progress.put(playerId, playerProgress);
            }

            if (playerSection.getBoolean("completed", false)) {
                completed.add(playerId);
            }
        }

        dirty = false;
    }

    private void savePersistedStateIfDirty() {
        if (!dirty) {
            return;
        }
        savePersistedState();
    }

    private void savePersistedState() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Unable to create plugin data folder for holiday gameplay persistence.");
            return;
        }

        YamlConfiguration yaml = new YamlConfiguration();
        if (activeHoliday != null) {
            String basePath = "holidays." + activeHoliday.name() + ".players";
            for (Map.Entry<UUID, Map<String, Integer>> entry : progress.entrySet()) {
                String playerPath = basePath + "." + entry.getKey();
                yaml.set(playerPath + ".progress", new HashMap<>(entry.getValue()));
                yaml.set(playerPath + ".completed", completed.contains(entry.getKey()));
            }
            for (UUID playerId : completed) {
                String playerPath = basePath + "." + playerId;
                if (yaml.get(playerPath + ".progress") == null) {
                    yaml.set(playerPath + ".progress", new HashMap<String, Integer>());
                }
                yaml.set(playerPath + ".completed", true);
            }
        }

        try {
            yaml.save(stateFile);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to save holiday gameplay state: " + ex.getMessage());
        }
    }

    private void clearPersistedState() {
        if (!stateFile.exists()) {
            dirty = false;
            return;
        }
        if (!stateFile.delete()) {
            plugin.getLogger().warning("Failed to clear holiday gameplay state file: " + stateFile.getName());
            return;
        }
        dirty = false;
    }
}
