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
        if (holidaysSection == null) {
            plugin.getLogger().warning("Holiday events config is missing the 'holidays' section.");
            return new HolidayEventConfig(definitions);
        }

        for (String holidayKey : holidaysSection.getKeys(false)) {
            Optional<HolidayType> holidayType = HolidayType.fromStringOrWarn(
                    holidayKey,
                    plugin.getLogger()::warning,
                    "holiday-events.yml:holidays"
            );
            if (holidayType.isEmpty()) {
                continue;
            }

            HolidayType type = holidayType.get();
            ConfigurationSection section = holidaysSection.getConfigurationSection(holidayKey);
            if (section == null) {
                plugin.getLogger().warning("Skipping holiday '" + holidayKey + "' because it is not a configuration section.");
                continue;
            }

            String eventName = section.getString("eventName", type.eventName());
            String objective = section.getString("objective", type.objective());
            HolidayEventZone zone = readZone(section.getConfigurationSection("zone"), defaultZone);

            List<HolidayTaskDefinition> tasks = new ArrayList<>();
            int taskIndex = 0;
            for (Map<?, ?> raw : section.getMapList("tasks")) {
                String context = "holiday '" + holidayKey + "' task #" + taskIndex;
                taskIndex++;

                String typeName = asString(raw.get("type"));
                String target = asString(raw.get("target"));
                int amount = parseInt(raw.get("amount"), 1);

                Optional<HolidayTaskType> taskType = HolidayTaskType.fromStringOrWarn(typeName, plugin.getLogger()::warning, context);
                if (taskType.isEmpty()) {
                    continue;
                }
                if (target.isBlank()) {
                    plugin.getLogger().warning("Skipping " + context + " because target is blank.");
                    continue;
                }
                if (amount <= 0) {
                    plugin.getLogger().warning("Skipping " + context + " because amount must be greater than 0.");
                    continue;
                }

                tasks.add(new HolidayTaskDefinition(taskType.get(), target.toUpperCase(Locale.ROOT), amount));
            }

            List<HolidayRewardDefinition> rewards = new ArrayList<>();
            int rewardIndex = 0;
            for (Map<?, ?> raw : section.getMapList("rewards")) {
                String context = "holiday '" + holidayKey + "' reward #" + rewardIndex;
                rewardIndex++;

                String typeName = asString(raw.get("type"));
                Optional<HolidayRewardType> rewardType = HolidayRewardType.fromStringOrWarn(typeName, plugin.getLogger()::warning, context);
                if (rewardType.isEmpty()) {
                    continue;
                }

                String target = asString(raw.get("target"));
                int amount = parseInt(raw.get("amount"), 1);
                String command = asString(raw.get("command"));
                double chance = parseDouble(raw.get("chance"), 1.0D);

                if (amount <= 0) {
                    plugin.getLogger().warning("Skipping " + context + " because amount must be greater than 0.");
                    continue;
                }

                if (chance <= 0.0D || chance > 1.0D) {
                    plugin.getLogger().warning("Skipping " + context + " because chance must be within (0.0, 1.0].");
                    continue;
                }

                if (rewardType.get() == HolidayRewardType.ITEM && Material.matchMaterial(target) == null) {
                    plugin.getLogger().warning("Skipping " + context + " because material target '" + target + "' is invalid.");
                    continue;
                }

                String rewardTarget = rewardType.get() == HolidayRewardType.CUSTOM_ITEM
                        ? target.trim().toLowerCase(Locale.ROOT)
                        : target.toUpperCase(Locale.ROOT);

                rewards.add(new HolidayRewardDefinition(
                        rewardType.get(),
                        rewardTarget,
                        Math.max(1, amount),
                        command,
                        chance
                ));
            }

            if (tasks.isEmpty() || rewards.isEmpty()) {
                plugin.getLogger().warning("Skipping holiday '" + holidayKey + "' because it must have at least one valid task and reward.");
                continue;
            }

            definitions.put(type, new HolidayEventDefinition(eventName, objective, zone, tasks, rewards));
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

    private static double parseDouble(Object raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String asString(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
