package com.lastbreath.hc.lastBreathHC.daily;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class DailyRewardManager {

    private static final String FILE_NAME = "daily-rewards.yml";

    private final LastBreathHC plugin;
    private final Map<UUID, DailyRewardData> cache = new HashMap<>();
    private final Map<Integer, List<DailyRewardAction>> rewardsByDay = new HashMap<>();
    private final Map<Integer, List<DailyRewardAction>> streakMilestones = new HashMap<>();
    private boolean notifyOnJoin;
    private boolean autoOpenOnFirstJoinOfDay;

    public DailyRewardManager(LastBreathHC plugin) {
        this.plugin = plugin;
        reloadRewardConfig();
    }

    public void reloadRewardConfig() {
        rewardsByDay.clear();
        streakMilestones.clear();

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("dailyRewards");
        notifyOnJoin = section == null || section.getBoolean("notifyOnJoin", true);
        autoOpenOnFirstJoinOfDay = section != null && section.getBoolean("autoOpenOnFirstJoinOfDay", false);

        if (section == null) {
            plugin.getLogger().warning("dailyRewards section missing in config.yml. Using fallback reward.");
            rewardsByDay.put(1, List.of(new ItemRewardAction(Material.COOKED_BEEF, 8)));
            return;
        }

        loadRewardsMap(section.getConfigurationSection("rewardsByDay"), rewardsByDay, "dailyRewards.rewardsByDay");
        loadRewardsMap(section.getConfigurationSection("streakMilestones"), streakMilestones, "dailyRewards.streakMilestones");

        if (rewardsByDay.isEmpty()) {
            plugin.getLogger().warning("No valid daily rewards found. Adding safe fallback reward.");
            rewardsByDay.put(1, List.of(new ItemRewardAction(Material.COOKED_BEEF, 8)));
        }
    }

    public DailyRewardData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadFromDisk);
    }

    public boolean canClaimToday(UUID uuid) {
        DailyRewardData data = get(uuid);
        return data.getLastClaimEpochDay() != epochDayNow();
    }

    public DailyClaimResult claim(Player player) {
        DailyRewardData data = get(player.getUniqueId());
        long today = epochDayNow();

        if (data.getLastClaimEpochDay() == today) {
            return new DailyClaimResult(DailyClaimStatus.ALREADY_CLAIMED, data.getCurrentStreak(), List.of());
        }

        boolean streakReset = data.getLastClaimEpochDay() >= 0 && data.getLastClaimEpochDay() < today - 1;
        int newStreak = data.getLastClaimEpochDay() == today - 1 ? data.getCurrentStreak() + 1 : 1;

        data.setCurrentStreak(newStreak);
        data.setMaxStreak(Math.max(data.getMaxStreak(), newStreak));
        data.setLastClaimEpochDay(today);
        save(player.getUniqueId());

        List<DailyRewardAction> rewards = getRewardsForDay(newStreak);
        List<DailyRewardAction> milestoneRewards = streakMilestones.getOrDefault(newStreak, List.of());
        List<String> granted = new ArrayList<>();

        for (DailyRewardAction reward : rewards) {
            granted.add(reward.grant(player));
        }
        for (DailyRewardAction reward : milestoneRewards) {
            granted.add(reward.grant(player));
        }

        DailyClaimStatus status = streakReset ? DailyClaimStatus.CLAIMED_STREAK_RESET : DailyClaimStatus.CLAIMED;
        return new DailyClaimResult(status, newStreak, granted);
    }

    public List<String> getPreview(Player player) {
        DailyRewardData data = get(player.getUniqueId());
        long today = epochDayNow();
        boolean canClaim = data.getLastClaimEpochDay() != today;
        int projectedStreak = data.getLastClaimEpochDay() == today - 1 ? data.getCurrentStreak() + 1 : 1;

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Current streak: " + ChatColor.GOLD + data.getCurrentStreak());
        lore.add(ChatColor.GRAY + "Best streak: " + ChatColor.GOLD + data.getMaxStreak());
        lore.add(canClaim
                ? ChatColor.GREEN + "You can claim today's reward."
                : ChatColor.RED + "Already claimed today.");
        lore.add(ChatColor.DARK_GRAY + "");
        lore.add(ChatColor.YELLOW + "Today's reward:");
        for (DailyRewardAction action : getRewardsForDay(projectedStreak)) {
            lore.add(action.preview());
        }

        if (streakMilestones.containsKey(projectedStreak)) {
            lore.add(ChatColor.AQUA + "Milestone bonus:");
            for (DailyRewardAction action : streakMilestones.get(projectedStreak)) {
                lore.add(action.preview());
            }
        }

        lore.add(ChatColor.DARK_GRAY + "");
        lore.add(ChatColor.YELLOW + "Upcoming milestones:");
        int shown = 0;
        for (Integer day : sortedMilestoneDays()) {
            if (day <= data.getCurrentStreak()) {
                continue;
            }
            lore.add(ChatColor.GRAY + "Day " + day + ChatColor.DARK_GRAY + " -> " + ChatColor.WHITE + summarize(streakMilestones.get(day)));
            shown++;
            if (shown >= 3) {
                break;
            }
        }
        if (shown == 0) {
            lore.add(ChatColor.DARK_GRAY + "No upcoming milestones.");
        }

        return lore;
    }

    public boolean markJoin(UUID uuid) {
        DailyRewardData data = get(uuid);
        long today = epochDayNow();
        Long lastJoin = data.getLastJoinEpochDay();
        boolean firstJoinToday = lastJoin == null || lastJoin != today;
        if (firstJoinToday) {
            data.setLastJoinEpochDay(today);
            save(uuid);
        }
        return firstJoinToday;
    }

    public boolean isNotifyOnJoin() {
        return notifyOnJoin;
    }

    public boolean isAutoOpenOnFirstJoinOfDay() {
        return autoOpenOnFirstJoinOfDay;
    }

    public void save(UUID uuid) {
        DailyRewardData data = cache.get(uuid);
        if (data == null) {
            return;
        }

        File file = getFile();
        ensureDirectory(file.getParentFile());
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        String base = "players." + uuid;
        config.set(base + ".lastClaimEpochDay", data.getLastClaimEpochDay());
        config.set(base + ".currentStreak", data.getCurrentStreak());
        config.set(base + ".maxStreak", data.getMaxStreak());
        config.set(base + ".lastJoinEpochDay", data.getLastJoinEpochDay());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save daily reward data: " + e.getMessage());
        }
    }

    public void saveAll() {
        File file = getFile();
        ensureDirectory(file.getParentFile());
        YamlConfiguration config = file.exists() ? YamlConfiguration.loadConfiguration(file) : new YamlConfiguration();

        for (DailyRewardData data : cache.values()) {
            String base = "players." + data.getUuid();
            config.set(base + ".lastClaimEpochDay", data.getLastClaimEpochDay());
            config.set(base + ".currentStreak", data.getCurrentStreak());
            config.set(base + ".maxStreak", data.getMaxStreak());
            config.set(base + ".lastJoinEpochDay", data.getLastJoinEpochDay());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Unable to save daily reward data: " + e.getMessage());
        }
    }

    private DailyRewardData loadFromDisk(UUID uuid) {
        DailyRewardData data = new DailyRewardData(uuid);
        File file = getFile();
        if (!file.exists()) {
            return data;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        String base = "players." + uuid;
        data.setLastClaimEpochDay(config.getLong(base + ".lastClaimEpochDay", -1L));
        data.setCurrentStreak(config.getInt(base + ".currentStreak", 0));
        data.setMaxStreak(config.getInt(base + ".maxStreak", 0));
        if (config.contains(base + ".lastJoinEpochDay")) {
            data.setLastJoinEpochDay(config.getLong(base + ".lastJoinEpochDay"));
        }
        return data;
    }

    private List<DailyRewardAction> getRewardsForDay(int streakDay) {
        if (rewardsByDay.containsKey(streakDay)) {
            return rewardsByDay.get(streakDay);
        }
        int maxConfiguredDay = rewardsByDay.keySet().stream().max(Integer::compareTo).orElse(1);
        int safeDay = Math.max(1, maxConfiguredDay);
        return rewardsByDay.getOrDefault(safeDay, List.of(new ItemRewardAction(Material.COOKED_BEEF, 8)));
    }

    private List<Integer> sortedMilestoneDays() {
        List<Integer> days = new ArrayList<>(streakMilestones.keySet());
        days.sort(Comparator.naturalOrder());
        return days;
    }

    private String summarize(List<DailyRewardAction> actions) {
        if (actions == null || actions.isEmpty()) {
            return "No reward";
        }
        return String.join(ChatColor.GRAY + ", " + ChatColor.WHITE,
                actions.stream().limit(2).map(action -> ChatColor.stripColor(action.preview())).toList());
    }

    private void loadRewardsMap(ConfigurationSection section,
                                Map<Integer, List<DailyRewardAction>> output,
                                String pathLabel) {
        if (section == null) {
            plugin.getLogger().warning(pathLabel + " is missing.");
            return;
        }

        for (String key : section.getKeys(false)) {
            int day;
            try {
                day = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("Skipping invalid day key in " + pathLabel + ": " + key);
                continue;
            }

            List<Map<?, ?>> rawList = section.getMapList(key);
            if (rawList.isEmpty()) {
                plugin.getLogger().warning("No rewards listed for day " + day + " in " + pathLabel);
                continue;
            }

            List<DailyRewardAction> parsed = new ArrayList<>();
            for (Map<?, ?> rawEntry : rawList) {
                DailyRewardAction action = parseReward(rawEntry, pathLabel + "." + key);
                if (action != null) {
                    parsed.add(action);
                }
            }

            if (!parsed.isEmpty()) {
                output.put(day, parsed);
            }
        }
    }

    private DailyRewardAction parseReward(Map<?, ?> raw, String pathLabel) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }

        Map<String, Object> entry = new LinkedHashMap<>();
        raw.forEach((k, v) -> entry.put(String.valueOf(k), v));

        String type = string(entry.getOrDefault("type", "ITEM")).toUpperCase(Locale.US);
        if ("ITEM".equals(type)) {
            Material material = Material.matchMaterial(string(entry.get("material")));
            if (material == null || material.isAir()) {
                plugin.getLogger().warning("Invalid material in " + pathLabel + ": " + entry.get("material"));
                return null;
            }
            int amount = integer(entry.getOrDefault("amount", 1), 1);
            return new ItemRewardAction(material, amount);
        }

        if ("EFFECT".equals(type)) {
            String effectName = string(entry.get("effect"));
            PotionEffectType effectType = PotionEffectType.getByName(effectName.toUpperCase(Locale.US));
            if (effectType == null) {
                plugin.getLogger().warning("Invalid potion effect in " + pathLabel + ": " + effectName);
                return null;
            }
            int durationSeconds = integer(entry.getOrDefault("durationSeconds", 30), 30);
            int amplifier = integer(entry.getOrDefault("amplifier", 0), 0);
            return new EffectRewardAction(effectType, durationSeconds, amplifier);
        }

        plugin.getLogger().warning("Unsupported reward type in " + pathLabel + ": " + type);
        return null;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private static int integer(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private long epochDayNow() {
        return LocalDate.now(ZoneOffset.UTC).toEpochDay();
    }

    private File getFile() {
        return new File(plugin.getDataFolder(), FILE_NAME);
    }

    private void ensureDirectory(File directory) {
        if (directory != null && !directory.exists() && !directory.mkdirs()) {
            plugin.getLogger().warning("Unable to create daily reward directory at " + directory.getAbsolutePath());
        }
    }
}
