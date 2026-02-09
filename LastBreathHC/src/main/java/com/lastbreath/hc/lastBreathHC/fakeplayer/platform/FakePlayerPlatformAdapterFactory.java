package com.lastbreath.hc.lastBreathHC.fakeplayer.platform;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;

import java.util.Locale;

public final class FakePlayerPlatformAdapterFactory {

    private FakePlayerPlatformAdapterFactory() {
    }

    public static FakePlayerPlatformAdapter create(LastBreathHC plugin) {
        String version = Bukkit.getMinecraftVersion().toLowerCase(Locale.ROOT);
        if (version.startsWith("1.21.11")) {
            return instantiate(plugin, "com.lastbreath.hc.lastBreathHC.fakeplayer.platform.v1_21_11.Paper12111FakePlayerAdapter");
        }
        plugin.getLogger().warning("No fake-player visual adapter available for Minecraft " + version + ".");
        return new NoOpFakePlayerPlatformAdapter();
    }

    private static FakePlayerPlatformAdapter instantiate(LastBreathHC plugin, String className) {
        try {
            Class<?> adapterClass = Class.forName(className);
            return (FakePlayerPlatformAdapter) adapterClass.getConstructor(LastBreathHC.class).newInstance(plugin);
        } catch (Exception exception) {
            plugin.getLogger().warning("Unable to initialize fake-player adapter " + className + ": " + exception.getMessage());
            return new NoOpFakePlayerPlatformAdapter();
        }
    }
}
