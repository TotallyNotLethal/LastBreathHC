package com.lastbreath.hc.lastBreathHC.nickname;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NicknameStorage {

    private static final String FILE_NAME = "nicknames.yml";

    public static String load(UUID uuid) {
        File file = getFile();
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.getString("players." + uuid + ".nickname");
    }

    public static void save(UUID uuid, String nickname) {
        File file = getFile();
        ensureDirectory(file.getParentFile());
        YamlConfiguration config = file.exists()
                ? YamlConfiguration.loadConfiguration(file)
                : new YamlConfiguration();
        String base = "players." + uuid + ".nickname";
        config.set(base, nickname == null || nickname.isBlank() ? null : nickname);
        try {
            config.save(file);
        } catch (IOException e) {
            LastBreathHC.getInstance().getLogger().warning("Unable to save nickname data: " + e.getMessage());
        }
    }

    private static File getFile() {
        return new File(LastBreathHC.getInstance().getDataFolder(), FILE_NAME);
    }

    private static void ensureDirectory(File directory) {
        if (directory != null && !directory.exists() && !directory.mkdirs()) {
            LastBreathHC.getInstance().getLogger().warning(
                    "Unable to create nickname directory at " + directory.getAbsolutePath()
            );
        }
    }
}
