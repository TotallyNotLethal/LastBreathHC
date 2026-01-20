package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class CustomPotionEffectRegistry {

    private final Map<String, CustomPotionEffectDefinition> effectsById;

    private CustomPotionEffectRegistry(Map<String, CustomPotionEffectDefinition> effectsById) {
        this.effectsById = effectsById;
    }

    public static CustomPotionEffectRegistry load(LastBreathHC plugin, String resourcePath) {
        Objects.requireNonNull(plugin, "plugin");
        Logger logger = plugin.getLogger();
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            logger.warning("Unable to create plugin data folder for custom potion effects.");
        }

        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> entries = config.getMapList("effects");
        Map<String, CustomPotionEffectDefinition> definitions = new HashMap<>();

        for (Map<?, ?> entry : entries) {
            String id = getString(entry, "id");
            String displayName = getString(entry, "displayName");
            String description = getString(entry, "description");
            String categoryName = getString(entry, "category");

            if (id == null || displayName == null || categoryName == null) {
                logger.warning("Skipping custom effect definition with missing id/displayName/category.");
                continue;
            }

            CustomEffectCategory category;
            try {
                category = CustomEffectCategory.valueOf(categoryName.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ex) {
                logger.warning("Skipping custom effect " + id + " due to invalid category: " + categoryName);
                continue;
            }

            CustomPotionEffectDefinition definition = new CustomPotionEffectDefinition(
                    id,
                    displayName,
                    description == null ? "" : description,
                    category
            );
            definitions.put(id, definition);
        }

        return new CustomPotionEffectRegistry(Collections.unmodifiableMap(definitions));
    }

    public CustomPotionEffectDefinition getById(String id) {
        return effectsById.get(id);
    }

    public List<CustomPotionEffectDefinition> getAll() {
        return new ArrayList<>(effectsById.values());
    }

    private static String getString(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public record CustomPotionEffectDefinition(String id,
                                               String displayName,
                                               String description,
                                               CustomEffectCategory category) {
    }
}
