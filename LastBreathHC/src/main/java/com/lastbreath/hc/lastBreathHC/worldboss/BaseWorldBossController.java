package com.lastbreath.hc.lastBreathHC.worldboss;

import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public abstract class BaseWorldBossController implements WorldBossController {

    protected final Plugin plugin;
    protected final LivingEntity boss;
    protected final NamespacedKey phaseKey;
    protected final NamespacedKey blockKey;
    protected final NamespacedKey dataKey;

    protected BaseWorldBossController(Plugin plugin, LivingEntity boss, String phaseKeyName, String blockKeyName, String dataKeyName) {
        this.plugin = plugin;
        this.boss = boss;
        this.phaseKey = new NamespacedKey(plugin, phaseKeyName);
        this.blockKey = new NamespacedKey(plugin, blockKeyName);
        this.dataKey = new NamespacedKey(plugin, dataKeyName);
    }

    @Override
    public LivingEntity getBoss() {
        return boss;
    }

    @Override
    public void handleBossDamaged(EntityDamageByEntityEvent event) {
    }

    @Override
    public void handleBossAttack(EntityDamageByEntityEvent event) {
    }

    @Override
    public void handleBlockBreak(BlockBreakEvent event) {
    }

    @Override
    public void onArenaEmpty() {
    }

    protected void setPhase(String phase) {
        boss.getPersistentDataContainer().set(phaseKey, PersistentDataType.STRING, phase);
    }

    protected String getPhase(String fallback) {
        String value = boss.getPersistentDataContainer().get(phaseKey, PersistentDataType.STRING);
        return value != null ? value : fallback;
    }

    protected void setData(String value) {
        boss.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, value);
    }

    protected String getData(String fallback) {
        String value = boss.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        return value != null ? value : fallback;
    }

    protected void storeBlockLocations(Set<Location> locations) {
        if (locations == null || locations.isEmpty()) {
            boss.getPersistentDataContainer().remove(blockKey);
            return;
        }
        List<String> encoded = new ArrayList<>();
        for (Location location : locations) {
            encoded.add(encodeLocation(location));
        }
        boss.getPersistentDataContainer().set(blockKey, PersistentDataType.STRING, String.join(";", encoded));
    }

    protected Set<Location> loadBlockLocations() {
        PersistentDataContainer container = boss.getPersistentDataContainer();
        String stored = container.get(blockKey, PersistentDataType.STRING);
        if (stored == null || stored.isBlank()) {
            return new HashSet<>();
        }
        Set<Location> locations = new HashSet<>();
        String[] parts = stored.split(";");
        for (String entry : parts) {
            Location location = decodeLocation(entry);
            if (location != null) {
                locations.add(location);
            }
        }
        return locations;
    }

    private String encodeLocation(Location location) {
        World world = location.getWorld();
        String worldName = world != null ? world.getName() : "world";
        return worldName + "|" + location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ();
    }

    private Location decodeLocation(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] worldSplit = value.split("\\|");
        if (worldSplit.length != 2) {
            return null;
        }
        String worldName = worldSplit[0].trim();
        String[] coords = worldSplit[1].split(",");
        if (coords.length != 3) {
            return null;
        }
        try {
            int x = Integer.parseInt(coords[0].trim());
            int y = Integer.parseInt(coords[1].trim());
            int z = Integer.parseInt(coords[2].trim());
            World world = plugin.getServer().getWorld(worldName);
            if (world == null) {
                return null;
            }
            return new Location(world, x, y, z);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    protected String serializeInts(int... values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                builder.append(",");
            }
            builder.append(values[i]);
        }
        return builder.toString();
    }

    protected int[] parseInts(String raw, int count) {
        int[] values = new int[count];
        if (raw == null || raw.isBlank()) {
            return values;
        }
        String[] parts = raw.toLowerCase(Locale.ROOT).split(",");
        for (int i = 0; i < Math.min(parts.length, count); i++) {
            try {
                values[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException ex) {
                values[i] = 0;
            }
        }
        return values;
    }
}
