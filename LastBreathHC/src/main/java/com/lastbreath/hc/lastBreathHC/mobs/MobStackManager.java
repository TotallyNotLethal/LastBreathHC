package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerTags;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.loot.Lootable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class MobStackManager {

    private static final int SEARCH_RADIUS = 10;
    private static final int ONE_BY_ONE_THRESHOLD = 5;
    private static final int THREE_BY_THREE_THRESHOLD = 45;
    private static final long SCAN_INTERVAL_TICKS = 100L;

    private final LastBreathHC plugin;
    private final NamespacedKey stackCountKey;
    private final NamespacedKey stackEnabledKey;
    private final NamespacedKey aiEnabledKey;
    private BukkitTask scanTask;

    public MobStackManager(LastBreathHC plugin) {
        this.plugin = plugin;
        this.stackCountKey = new NamespacedKey(plugin, "mob_stack_count");
        this.stackEnabledKey = new NamespacedKey(plugin, "stack_enabled");
        this.aiEnabledKey = new NamespacedKey(plugin, "ai_enabled");
    }

    public void start() {
        if (scanTask != null) {
            scanTask.cancel();
        }
        scanTask = new BukkitRunnable() {
            @Override
            public void run() {
                scanAndStack();
            }
        }.runTaskTimer(plugin, SCAN_INTERVAL_TICKS, SCAN_INTERVAL_TICKS);
    }

    public void stop() {
        if (scanTask != null) {
            scanTask.cancel();
            scanTask = null;
        }
    }

    public boolean isStacked(LivingEntity entity) {
        return getStackCount(entity) > 1;
    }

    public int getStackCount(LivingEntity entity) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        Integer stored = container.get(stackCountKey, PersistentDataType.INTEGER);
        if (stored == null || stored < 1) {
            return 1;
        }
        return stored;
    }

    public void setStackCount(LivingEntity entity, int count) {
        PersistentDataContainer container = entity.getPersistentDataContainer();
        if (count <= 1) {
            container.remove(stackCountKey);
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }
        container.set(stackCountKey, PersistentDataType.INTEGER, count);
        updateCustomName(entity);
    }

    public void incrementStack(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return;
        }
        setStackCount(entity, getStackCount(entity) + amount);
    }

    public void decrementStack(LivingEntity entity, int amount) {
        if (amount <= 0) {
            return;
        }
        int updated = getStackCount(entity) - amount;
        if (updated <= 0) {
            entity.remove();
            return;
        }
        setStackCount(entity, updated);
    }

    public void updateCustomName(LivingEntity entity) {
        int count = getStackCount(entity);
        if (count <= 1) {
            entity.setCustomName(null);
            entity.setCustomNameVisible(false);
            return;
        }
        String name = formatEntityName(entity.getType());
        entity.setCustomName(name + " x" + count);
        entity.setCustomNameVisible(true);
    }

    public void applyLootForKills(LivingEntity entity, Player killer, int kills) {
        if (kills <= 0) {
            return;
        }
        for (int i = 0; i < kills; i++) {
            for (ItemStack drop : getDropsForKill(entity, killer)) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), drop);
            }
            dropExperience(entity);
        }
    }

    private void scanAndStack() {
        for (World world : Bukkit.getWorlds()) {
            List<LivingEntity> tagged = world.getLivingEntities().stream()
                    .filter(this::isPlayerSpawnerMob)
                    .toList();
            if (tagged.isEmpty()) {
                continue;
            }

            Map<BlockKey, Boolean> stackAllowedCache = new HashMap<>();
            Map<BlockKey, Boolean> aiEnabledCache = new HashMap<>();
            Map<StackGroupKey, List<LivingEntity>> grouped = new HashMap<>();

            for (LivingEntity entity : tagged) {
                BlockKey blockKey = BlockKey.from(entity.getLocation());
                boolean aiEnabled = aiEnabledCache.computeIfAbsent(blockKey, key -> resolveAiEnabled(entity.getLocation()));
                applyAiSetting(entity, aiEnabled);

                boolean stackAllowed = stackAllowedCache.computeIfAbsent(blockKey, key -> resolveStackingAllowed(entity.getLocation()));
                if (!stackAllowed) {
                    continue;
                }

                StackGroupKey key = new StackGroupKey(entity.getType(), blockKey.x(), blockKey.z());
                grouped.computeIfAbsent(key, ignored -> new ArrayList<>()).add(entity);
            }

            Set<LivingEntity> processed = new HashSet<>();

            for (List<LivingEntity> entities : grouped.values()) {
                int totalCount = getTotalStackCount(entities);
                if (totalCount < ONE_BY_ONE_THRESHOLD || entities.size() <= 1) {
                    continue;
                }
                mergeEntities(entities, totalCount);
                processed.addAll(entities);
            }

            for (Map.Entry<StackGroupKey, List<LivingEntity>> entry : grouped.entrySet()) {
                StackGroupKey center = entry.getKey();
                if (entry.getValue().stream().allMatch(processed::contains)) {
                    continue;
                }
                List<LivingEntity> region = new ArrayList<>();
                int totalCount = 0;
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        StackGroupKey neighbor = new StackGroupKey(center.type(), center.x() + dx, center.z() + dz);
                        List<LivingEntity> neighbors = grouped.get(neighbor);
                        if (neighbors == null) {
                            continue;
                        }
                        for (LivingEntity entity : neighbors) {
                            if (processed.contains(entity)) {
                                continue;
                            }
                            region.add(entity);
                            totalCount += getStackCount(entity);
                        }
                    }
                }

                if (totalCount < THREE_BY_THREE_THRESHOLD || region.size() <= 1) {
                    continue;
                }
                mergeEntities(region, totalCount);
                processed.addAll(region);
            }
        }
    }

    private void mergeEntities(List<LivingEntity> entities, int totalCount) {
        if (entities.isEmpty()) {
            return;
        }
        LivingEntity representative = entities.get(0);
        representative.addScoreboardTag(SpawnerTags.PLAYER_SPAWNER_MOB_TAG);
        setStackCount(representative, totalCount);
        for (LivingEntity entity : entities) {
            if (entity.equals(representative)) {
                continue;
            }
            entity.remove();
        }
    }

    private int getTotalStackCount(List<LivingEntity> entities) {
        int total = 0;
        for (LivingEntity entity : entities) {
            total += getStackCount(entity);
        }
        return total;
    }

    private boolean isPlayerSpawnerMob(LivingEntity entity) {
        return entity.getScoreboardTags().contains(SpawnerTags.PLAYER_SPAWNER_MOB_TAG);
    }

    private boolean resolveStackingAllowed(Location location) {
        Boolean state = findSignState(location, stackEnabledKey);
        if (state == null) {
            return true;
        }
        return state;
    }

    private boolean resolveAiEnabled(Location location) {
        Boolean state = findSignState(location, aiEnabledKey);
        if (state == null) {
            return true;
        }
        return state;
    }

    private Boolean findSignState(Location origin, NamespacedKey key) {
        if (origin == null || origin.getWorld() == null) {
            return null;
        }
        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();
        int radiusSquared = SEARCH_RADIUS * SEARCH_RADIUS;
        World world = origin.getWorld();

        for (int x = originX - SEARCH_RADIUS; x <= originX + SEARCH_RADIUS; x++) {
            for (int y = originY - SEARCH_RADIUS; y <= originY + SEARCH_RADIUS; y++) {
                for (int z = originZ - SEARCH_RADIUS; z <= originZ + SEARCH_RADIUS; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    if (origin.distanceSquared(block.getLocation()) > radiusSquared) {
                        continue;
                    }
                    if (!(block.getState() instanceof Sign sign)) {
                        continue;
                    }
                    PersistentDataContainer container = sign.getPersistentDataContainer();
                    if (!container.has(key, PersistentDataType.BYTE)) {
                        continue;
                    }
                    Byte value = container.get(key, PersistentDataType.BYTE);
                    return value != null && value > 0;
                }
            }
        }
        return null;
    }

    private void applyAiSetting(LivingEntity entity, boolean aiEnabled) {
        if (!(entity instanceof Mob mob)) {
            return;
        }
        if (mob.hasAI() != aiEnabled) {
            mob.setAI(aiEnabled);
        }
    }

    private List<ItemStack> getDropsForKill(LivingEntity entity, Player killer) {
        if (entity instanceof Lootable lootable) {
            LootTable table = lootable.getLootTable();
            if (table != null) {
                LootContext.Builder builder = new LootContext.Builder(entity.getLocation());
                builder.lootedEntity(entity);
                double luck = 0.0;
                if (killer != null) {
                    var attr = killer.getAttribute(org.bukkit.attribute.Attribute.LUCK);
                    if (attr != null) {
                        luck = attr.getValue();
                    }
                }
                builder.luck((float) luck);

                return new ArrayList<>(table.populateLoot(
                        ThreadLocalRandom.current(),
                        builder.build()
                ));
            }
        }
        return List.of();
    }

    private void dropExperience(LivingEntity entity) {
        if (!(entity instanceof Mob mob)) {
            return;
        }

        int exp = mob.getPossibleExperienceReward();
        if (exp <= 0) return;

        ExperienceOrb orb = entity.getWorld().spawn(entity.getLocation(), ExperienceOrb.class);
        orb.setExperience(exp);
    }


    private String formatEntityName(EntityType type) {
        String[] words = type.name().toLowerCase(Locale.US).split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

    private record BlockKey(java.util.UUID worldId, int x, int y, int z) {
        public static BlockKey from(Location location) {
            return new BlockKey(
                    location.getWorld() == null ? new java.util.UUID(0L, 0L) : location.getWorld().getUID(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }
    }

    private record StackGroupKey(EntityType type, int x, int z) {
    }
}
