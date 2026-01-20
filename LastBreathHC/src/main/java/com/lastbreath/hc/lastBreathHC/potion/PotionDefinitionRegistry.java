package com.lastbreath.hc.lastBreathHC.potion;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

public class PotionDefinitionRegistry {

    private final Map<Material, HardcorePotionDefinition> definitionsByIngredient;
    private final Map<String, HardcorePotionDefinition> definitionsById;

    private PotionDefinitionRegistry(Map<Material, HardcorePotionDefinition> definitionsByIngredient,
                                     Map<String, HardcorePotionDefinition> definitionsById) {
        this.definitionsByIngredient = definitionsByIngredient;
        this.definitionsById = definitionsById;
    }

    public static PotionDefinitionRegistry load(LastBreathHC plugin, String resourcePath) {
        Objects.requireNonNull(plugin, "plugin");
        Logger logger = plugin.getLogger();
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            logger.warning("Unable to create plugin data folder for potion definitions.");
        }

        File file = new File(plugin.getDataFolder(), resourcePath);
        if (!file.exists()) {
            plugin.saveResource(resourcePath, false);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<Map<?, ?>> potionEntries = config.getMapList("potions");
        Map<Material, HardcorePotionDefinition> byIngredient = new HashMap<>();
        Map<String, HardcorePotionDefinition> byId = new HashMap<>();

        for (Map<?, ?> entry : potionEntries) {
            String id = getString(entry, "id");
            String displayName = getString(entry, "displayName");
            String ingredientName = getString(entry, "craftingIngredient");
            boolean scaryToDrink = getBoolean(entry, "scaryToDrink");

            if (id == null || displayName == null || ingredientName == null) {
                logger.warning("Skipping potion definition with missing id/displayName/craftingIngredient.");
                continue;
            }

            Material ingredient = Material.matchMaterial(ingredientName);
            if (ingredient == null) {
                logger.warning("Skipping potion definition " + id + " due to invalid ingredient: " + ingredientName);
                continue;
            }

            List<HardcorePotionDefinition.EffectDefinition> baseEffects = readEffects(entry, "baseEffects", EffectTrigger.ON_DRINK);
            List<HardcorePotionDefinition.EffectDefinition> drawbacks = readEffects(entry, "drawbacks", EffectTrigger.ON_DRINK);
            List<HardcorePotionDefinition.EffectDefinition> afterEffects = readEffects(entry, "afterEffects", EffectTrigger.AFTER_EFFECT);
            List<HardcorePotionDefinition.CustomEffectDefinition> customEffects = readCustomEffects(entry, "customEffects");
            List<String> branches = readStringList(entry, "branches");

            HardcorePotionDefinition definition = new HardcorePotionDefinition(
                    id,
                    displayName,
                    ingredient,
                    scaryToDrink,
                    baseEffects,
                    drawbacks,
                    afterEffects,
                    customEffects,
                    branches
            );

            if (byIngredient.containsKey(ingredient)) {
                logger.warning("Potion ingredient " + ingredient + " already mapped. Overwriting with " + id + ".");
            }
            if (byId.containsKey(id)) {
                logger.warning("Potion id " + id + " already defined. Overwriting.");
            }
            byIngredient.put(ingredient, definition);
            byId.put(id, definition);
        }

        return new PotionDefinitionRegistry(Collections.unmodifiableMap(byIngredient), Collections.unmodifiableMap(byId));
    }

    public HardcorePotionDefinition getByIngredient(Material ingredient) {
        return definitionsByIngredient.get(ingredient);
    }

    public HardcorePotionDefinition getById(String id) {
        return definitionsById.get(id);
    }

    public List<HardcorePotionDefinition> getAll() {
        return new ArrayList<>(definitionsById.values());
    }

    private static List<HardcorePotionDefinition.EffectDefinition> readEffects(Map<?, ?> potionEntry,
                                                                               String key,
                                                                               EffectTrigger defaultTrigger) {
        Object rawList = potionEntry.get(key);
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }
        List<Map<?, ?>> entries = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                entries.add(map);
            }
        }
        if (entries.isEmpty()) {
            return List.of();
        }
        List<HardcorePotionDefinition.EffectDefinition> effects = new ArrayList<>();
        for (Map<?, ?> effectEntry : entries) {
            String typeName = getString(effectEntry, "type");
            if (typeName == null) {
                continue;
            }
            PotionEffectType type = PotionEffectType.getByName(typeName.toUpperCase(Locale.ROOT));
            if (type == null) {
                continue;
            }
            int durationTicks = getInt(effectEntry, "durationTicks");
            int amplifier = getInt(effectEntry, "amplifier");
            EffectTrigger trigger = EffectTrigger.fromString(getString(effectEntry, "trigger"), defaultTrigger);
            int cooldownTicks = getInt(effectEntry, "cooldownTicks");
            effects.add(new HardcorePotionDefinition.EffectDefinition(type, durationTicks, amplifier, trigger, cooldownTicks));
        }
        return effects;
    }

    private static List<HardcorePotionDefinition.CustomEffectDefinition> readCustomEffects(Map<?, ?> potionEntry,
                                                                                           String key) {
        Object rawList = potionEntry.get(key);
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }
        List<Map<?, ?>> entries = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                entries.add(map);
            }
        }
        if (entries.isEmpty()) {
            return List.of();
        }
        List<HardcorePotionDefinition.CustomEffectDefinition> effects = new ArrayList<>();
        for (Map<?, ?> effectEntry : entries) {
            String id = getString(effectEntry, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            int durationTicks = getInt(effectEntry, "durationTicks");
            EffectTrigger trigger = EffectTrigger.fromString(getString(effectEntry, "trigger"), EffectTrigger.ON_DRINK);
            int cooldownTicks = getInt(effectEntry, "cooldownTicks");
            effects.add(new HardcorePotionDefinition.CustomEffectDefinition(id, durationTicks, trigger, cooldownTicks));
        }
        return effects;
    }

    private static String getString(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    private static boolean getBoolean(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private static int getInt(Map<?, ?> entry, String key) {
        Object value = entry.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static List<String> readStringList(Map<?, ?> entry, String key) {
        Object rawList = entry.get(key);
        if (!(rawList instanceof List<?> list)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (Object item : list) {
            if (item == null) {
                continue;
            }
            String value = item.toString();
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return values;
    }
}
