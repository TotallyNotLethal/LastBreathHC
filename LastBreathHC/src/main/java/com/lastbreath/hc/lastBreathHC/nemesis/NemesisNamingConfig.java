package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class NemesisNamingConfig {
    private static final String RESOURCE_NAME = "nemesis-naming.yml";

    private final YamlConfiguration namingConfig;
    private final YamlConfiguration namingDefaults;

    NemesisNamingConfig(LastBreathHC plugin) {
        plugin.saveResource(RESOURCE_NAME, false);
        this.namingConfig = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), RESOURCE_NAME));
        this.namingDefaults = loadDefaults(plugin);
    }

    private YamlConfiguration loadDefaults(LastBreathHC plugin) {
        YamlConfiguration defaults = new YamlConfiguration();
        try (InputStream bundledResource = java.util.Objects.requireNonNull(plugin.getResource(RESOURCE_NAME),
                "Missing bundled resource " + RESOURCE_NAME);
             InputStreamReader reader = new InputStreamReader(bundledResource, StandardCharsets.UTF_8)) {
            defaults.load(reader);
            return defaults;
        } catch (IOException | org.bukkit.configuration.InvalidConfigurationException ex) {
            throw new IllegalStateException("Failed to load bundled " + RESOURCE_NAME, ex);
        }
    }

    ConfigurationSection getSection(String path) {
        ConfigurationSection section = namingConfig.getConfigurationSection(path);
        if (section != null && !section.getKeys(false).isEmpty()) {
            return section;
        }

        ConfigurationSection defaultSection = namingDefaults.getConfigurationSection(path);
        if (defaultSection != null && !defaultSection.getKeys(false).isEmpty()) {
            return defaultSection;
        }

        return section;
    }
}
