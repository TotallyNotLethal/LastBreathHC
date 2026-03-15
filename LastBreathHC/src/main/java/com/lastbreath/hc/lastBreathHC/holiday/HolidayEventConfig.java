package com.lastbreath.hc.lastBreathHC.holiday;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HolidayEventConfig {

    private final Map<HolidayType, HolidayEventDefinition> definitions;

    private HolidayEventConfig(Map<HolidayType, HolidayEventDefinition> definitions) {
        this.definitions = definitions;
    }

    public HolidayEventDefinition definitionFor(HolidayType type) {
        return definitions.get(type);
    }

    public static HolidayEventConfig load(JavaPlugin plugin) {
        plugin.saveResource("holiday-events.yml", false);
        File file = new File(plugin.getDataFolder(), "holiday-events.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        HolidayEventZone defaultZone = readZone(config.getConfigurationSection("defaultZone"), new HolidayEventZone("world", 0, 64, 0, 300));
        Map<HolidayType, HolidayEventDefinition> definitions = new EnumMap<>(HolidayType.class);

        ConfigurationSection holidaysSection = config.getConfigurationSection("holidays");
        for (HolidayType type : HolidayType.values()) {
            String holidayKey = type.name().toLowerCase(Locale.ROOT);
            ConfigurationSection section = holidaysSection != null ? holidaysSection.getConfigurationSection(holidayKey) : null;
            if (section == null) {
                continue;
            }
            String eventName = section.getString("eventName", type.eventName());
            String objective = section.getString("objective", type.objective());
            HolidayEventZone zone = readZone(section.getConfigurationSection("zone"), defaultZone);

            List<HolidayTaskDefinition> tasks = new ArrayList<>();

            for (Map<?, ?> raw : section.getMapList("tasks")) {
                Map<String, Object> taskMap = (Map<String, Object>) raw;

                String typeName = String.valueOf(taskMap.getOrDefault("type", ""));
                String target = String.valueOf(taskMap.getOrDefault("target", ""));
                int amount = parseInt(taskMap.get("amount"), 1);

                if (typeName.isBlank() || target.isBlank() || amount <= 0) {
                    continue;
                }

                Optional<HolidayTaskType> taskType = HolidayTaskType.fromString(typeName);
                if (taskType.isEmpty()) {
                    plugin.getLogger().warning("Skipping holiday task for " + holidayKey + " due to invalid task type: " + typeName);
                    continue;
                }

                tasks.add(new HolidayTaskDefinition(
                        taskType.get(),
                        target.toUpperCase(Locale.ROOT),
                        amount
                ));
            }

            List<HolidayRewardDefinition> rewards = new ArrayList<>();

            for (Map<?, ?> raw : section.getMapList("rewards")) {
                Map<String, Object> rewardMap = (Map<String, Object>) raw;

                String typeName = String.valueOf(rewardMap.getOrDefault("type", ""));
                if (typeName.isBlank()) {
                    continue;
                }

                Optional<HolidayRewardType> rewardType = HolidayRewardType.fromString(typeName);
                if (rewardType.isEmpty()) {
                    plugin.getLogger().warning("Skipping holiday reward for " + holidayKey + " due to invalid reward type: " + typeName);
                    continue;
                }

                String target = String.valueOf(rewardMap.getOrDefault("target", ""));
                int amount = parseInt(rewardMap.get("amount"), 1);
                String command = String.valueOf(rewardMap.getOrDefault("command", ""));

                if (rewardType.get() == HolidayRewardType.ITEM && Material.matchMaterial(target) == null) {
                    continue;
                }

                rewards.add(new HolidayRewardDefinition(
                        rewardType.get(),
                        target.toUpperCase(Locale.ROOT),
                        Math.max(1, amount),
                        command
                ));
            }

            if (!tasks.isEmpty() && !rewards.isEmpty()) {
                definitions.put(type, new HolidayEventDefinition(eventName, objective, zone, tasks, rewards));
            }
        }

        return new HolidayEventConfig(definitions);
    }

    private static HolidayEventZone readZone(ConfigurationSection section, HolidayEventZone fallback) {
        if (section == null) {
            return fallback;
        }
        String world = section.getString("world", fallback.world());
        double x = section.getDouble("x", fallback.x());
        double y = section.getDouble("y", fallback.y());
        double z = section.getDouble("z", fallback.z());
        double radius = section.getDouble("radius", fallback.radius());
        return new HolidayEventZone(world, x, y, z, radius);
    }

    private static int parseInt(Object raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
