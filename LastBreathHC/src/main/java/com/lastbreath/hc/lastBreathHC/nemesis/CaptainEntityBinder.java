package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class CaptainEntityBinder {
    public static final String CAPTAIN_SCOREBOARD_TAG = "LB_NEMESIS_CAPTAIN";

    private final LastBreathHC plugin;
    private final CaptainRegistry captainRegistry;
    private final NamespacedKey captainIdKey;
    private CaptainTraitService traitService;
    private final CaptainStateMachine stateMachine = new CaptainStateMachine();

    private final double baseHealth;
    private final double healthPerLevel;
    private final double damagePerLevel;
    private final NavigableMap<Integer, Integer> minTierByLevel = new TreeMap<>();

    public CaptainEntityBinder(LastBreathHC plugin, CaptainRegistry captainRegistry) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.captainIdKey = new NamespacedKey(plugin, "nemesis_captain_uuid");

        this.baseHealth = Math.max(1.0, plugin.getConfig().getDouble("nemesis.scaling.baseHealth", 30.0));
        this.healthPerLevel = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scaling.healthPerLevel", 5.0));
        this.damagePerLevel = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scaling.damagePerLevel", 1.0));
        loadMinTierByLevelConfig();
    }

    public Optional<LivingEntity> spawnOrBind(CaptainRecord record) {
        if (record == null || record.identity() == null || record.origin() == null) {
            transitionToDormantOrCooldown(record);
            return Optional.empty();
        }

        LivingEntity upgradeCandidate = resolveLiveKillerEntity(record);
        if (upgradeCandidate != null && isChunkLoaded(upgradeCandidate.getLocation())) {
            bind(upgradeCandidate, record);
            return Optional.of(upgradeCandidate);
        }

        Location spawnLocation = buildLocation(record.origin());
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            transitionToDormantOrCooldown(record);
            return Optional.empty();
        }

        if (!isChunkLoaded(spawnLocation)) {
            transitionToDormantOrCooldown(record);
            return Optional.empty();
        }

        Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, resolveEntityType(record));
        if (!(entity instanceof LivingEntity livingEntity)) {
            entity.remove();
            transitionToDormantOrCooldown(record);
            return Optional.empty();
        }

        bind(livingEntity, record);
        return Optional.of(livingEntity);
    }

    public boolean tryUpgradeInPlace(CaptainRecord record) {
        LivingEntity live = resolveLiveKillerEntity(record);
        if (live == null) {
            return false;
        }
        bind(live, record, true);
        return true;
    }

    public Optional<CaptainRecord> resolveCaptainRecord(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity)) {
            return Optional.empty();
        }

        PersistentDataContainer pdc = livingEntity.getPersistentDataContainer();
        String captainId = pdc.get(captainIdKey, PersistentDataType.STRING);
        if (captainId == null || captainId.isBlank()) {
            return Optional.empty();
        }

        try {
            UUID captainUuid = UUID.fromString(captainId);
            return Optional.ofNullable(captainRegistry.getByCaptainUuid(captainUuid));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public void bind(LivingEntity entity, CaptainRecord record) {
        bind(entity, record, false);
    }

    private void bind(LivingEntity entity, CaptainRecord record, boolean preserveRuntimeState) {
        if (entity == null || record == null || record.identity() == null || record.identity().captainId() == null) {
            return;
        }

        entity.addScoreboardTag(CAPTAIN_SCOREBOARD_TAG);
        entity.getPersistentDataContainer().set(captainIdKey, PersistentDataType.STRING, record.identity().captainId().toString());

        String customName = formatCaptainName(record);
        entity.customName(net.kyori.adventure.text.Component.text(customName));
        entity.setCustomNameVisible(true);

        applyAttributes(entity, record, preserveRuntimeState);
        applyEquipmentRolls(entity, record);
        applyTraitHooks(entity, record);
        if (traitService != null) {
            traitService.applyOnBind(entity, record);
        }
    }

    private void applyAttributes(LivingEntity entity, CaptainRecord record, boolean preserveRuntimeState) {
        int level = Math.max(1, record.progression() == null ? 1 : record.progression().level());
        double tierMultiplier = tierMultiplier(record.progression() == null ? "COMMON" : record.progression().tier());

        double targetHealth = Math.max(1.0, (baseHealth + (level - 1) * healthPerLevel) * tierMultiplier);
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            double currentHealth = Math.max(0.0, entity.getHealth());
            maxHealthAttribute.setBaseValue(targetHealth);
            entity.setHealth(preserveRuntimeState ? Math.min(currentHealth, targetHealth) : targetHealth);
        }

        AttributeInstance attackDamageAttribute = entity.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackDamageAttribute != null) {
            double current = Math.max(1.0, attackDamageAttribute.getBaseValue());
            double scaledDamage = Math.max(current, (current + (level - 1) * damagePerLevel) * tierMultiplier);
            attackDamageAttribute.setBaseValue(scaledDamage);
        }
    }

    private void applyEquipmentRolls(LivingEntity entity, CaptainRecord record) {
        if (!(entity instanceof Mob mob)) {
            return;
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }

        int level = Math.max(1, record.progression() == null ? 1 : record.progression().level());
        Material helmetMaterial = rollArmorMaterial(level, ArmorSlot.HELMET);
        Material chestplateMaterial = rollArmorMaterial(level, ArmorSlot.CHESTPLATE);
        Material leggingsMaterial = rollArmorMaterial(level, ArmorSlot.LEGGINGS);
        Material bootsMaterial = rollArmorMaterial(level, ArmorSlot.BOOTS);

        ItemStack helmet = new ItemStack(helmetMaterial);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Nemesis Crown");
            helmet.setItemMeta(meta);
        }

        equipment.setHelmet(helmet);
        equipment.setChestplate(new ItemStack(chestplateMaterial));
        equipment.setLeggings(new ItemStack(leggingsMaterial));
        equipment.setBoots(new ItemStack(bootsMaterial));

        equipment.setItemInMainHand(new ItemStack(rollMainHandMaterial(level)));
        equipment.setItemInOffHand(new ItemStack(rollOffHandMaterial(level)));
    }

    private Material rollArmorMaterial(int level, ArmorSlot slot) {
        Material[][] tieredArmor = new Material[][]{
                {Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS},
                {
                        resolveArmorMaterial("COPPER_HELMET", Material.CHAINMAIL_HELMET),
                        resolveArmorMaterial("COPPER_CHESTPLATE", Material.CHAINMAIL_CHESTPLATE),
                        resolveArmorMaterial("COPPER_LEGGINGS", Material.CHAINMAIL_LEGGINGS),
                        resolveArmorMaterial("COPPER_BOOTS", Material.CHAINMAIL_BOOTS)
                },
                {Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS},
                {Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS},
                {Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS},
                {Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS}
        };

        int rolledTier = rollTierForLevel(level, tieredArmor.length);
        return tieredArmor[rolledTier][slot.ordinal()];
    }


    private Material rollMainHandMaterial(int level) {
        Material[][] tieredMainHandItems = new Material[][]{
                {
                        Material.WOODEN_SWORD,
                        Material.WOODEN_AXE,
                        Material.WOODEN_PICKAXE,
                        Material.WOODEN_SHOVEL,
                        Material.WOODEN_HOE,
                        Material.BUCKET
                },
                {
                        resolveMaterial("STONE_SWORD", Material.STONE_AXE),
                        Material.STONE_AXE,
                        Material.STONE_PICKAXE,
                        Material.STONE_SHOVEL,
                        Material.STONE_HOE,
                        Material.BUCKET
                },
                {
                        Material.GOLDEN_SWORD,
                        Material.GOLDEN_AXE,
                        Material.GOLDEN_PICKAXE,
                        Material.GOLDEN_SHOVEL,
                        Material.GOLDEN_HOE,
                        Material.BUCKET
                },
                {
                        Material.IRON_SWORD,
                        Material.IRON_AXE,
                        Material.IRON_PICKAXE,
                        Material.IRON_SHOVEL,
                        Material.IRON_HOE,
                        resolveMaterial("TRIDENT", Material.IRON_SWORD),
                        Material.BUCKET
                },
                {
                        Material.DIAMOND_SWORD,
                        Material.DIAMOND_AXE,
                        Material.DIAMOND_PICKAXE,
                        Material.DIAMOND_SHOVEL,
                        Material.DIAMOND_HOE,
                        resolveMaterial("TRIDENT", Material.DIAMOND_SWORD),
                        Material.BUCKET
                },
                {
                        Material.NETHERITE_SWORD,
                        Material.NETHERITE_AXE,
                        Material.NETHERITE_PICKAXE,
                        Material.NETHERITE_SHOVEL,
                        Material.NETHERITE_HOE,
                        resolveMaterial("TRIDENT", Material.NETHERITE_SWORD),
                        resolveMaterial("MACE", Material.NETHERITE_SWORD),
                        Material.BUCKET
                }
        };

        int tier = rollTierForLevel(level, tieredMainHandItems.length);
        Material[] candidates = tieredMainHandItems[tier];
        return candidates[ThreadLocalRandom.current().nextInt(candidates.length)];
    }

    private Material rollOffHandMaterial(int level) {
        double utilityChance = Math.min(0.8, 0.25 + (level * 0.03));
        if (ThreadLocalRandom.current().nextDouble() > utilityChance) {
            return Material.AIR;
        }

        Material[] utility = {
                Material.BUCKET,
                Material.WATER_BUCKET,
                Material.LAVA_BUCKET,
                resolveMaterial("POWDER_SNOW_BUCKET", Material.BUCKET)
        };
        return utility[ThreadLocalRandom.current().nextInt(utility.length)];
    }

    private int rollTierForLevel(int level, int tierCount) {
        int targetTier = Math.min(tierCount - 1, Math.max(0, (level - 1) / 4));
        int minTierFloor = resolveMinTierFloor(level, tierCount);
        int[] weights = new int[tierCount];
        int totalWeight = 0;
        for (int tier = 0; tier < tierCount; tier++) {
            if (tier < minTierFloor) {
                weights[tier] = 0;
                continue;
            }
            int distance = Math.abs(tier - targetTier);
            int weight = switch (distance) {
                case 0 -> 55;
                case 1 -> 24;
                case 2 -> 12;
                case 3 -> 6;
                default -> 3;
            };
            weights[tier] = weight;
            totalWeight += weight;
        }

        int roll = ThreadLocalRandom.current().nextInt(totalWeight);
        for (int tier = 0; tier < tierCount; tier++) {
            if (weights[tier] <= 0) {
                continue;
            }
            roll -= weights[tier];
            if (roll < 0) {
                return tier;
            }
        }
        return Math.max(minTierFloor, targetTier);
    }

    private void loadMinTierByLevelConfig() {
        minTierByLevel.clear();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("nemesis.equipment.minTierByLevel");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                int levelFloor = Integer.parseInt(key);
                int tierFloor = Math.max(0, section.getInt(key, 0));
                minTierByLevel.put(levelFloor, tierFloor);
            } catch (NumberFormatException ignored) {
                // Ignore malformed config key.
            }
        }
    }

    private int resolveMinTierFloor(int level, int tierCount) {
        if (minTierByLevel.isEmpty()) {
            return 0;
        }

        Map.Entry<Integer, Integer> floorEntry = minTierByLevel.floorEntry(Math.max(1, level));
        if (floorEntry == null) {
            return 0;
        }

        return Math.min(tierCount - 1, Math.max(0, floorEntry.getValue()));
    }

    private void applyTraitHooks(LivingEntity entity, CaptainRecord record) {
        if (record.traits() == null || record.traits().traits() == null) {
            return;
        }

        for (String trait : record.traits().traits()) {
            // Hook point for future trait system integrations.
            if (Objects.equals(trait, "speedy")) {
                AttributeInstance movement = entity.getAttribute(Attribute.MOVEMENT_SPEED);
                if (movement != null) {
                    movement.setBaseValue(movement.getBaseValue() * 1.1);
                }
            }
        }
    }

    private Material resolveArmorMaterial(String candidate, Material fallback) {
        return resolveMaterial(candidate, fallback);
    }

    private Material resolveMaterial(String candidate, Material fallback) {
        Material resolved = Material.matchMaterial(candidate);
        return resolved == null ? fallback : resolved;
    }

    private enum ArmorSlot {
        HELMET,
        CHESTPLATE,
        LEGGINGS,
        BOOTS
    }

    private double tierMultiplier(String tier) {
        String normalized = tier == null ? "common" : tier.toLowerCase(Locale.ROOT);
        return Math.max(0.1, plugin.getConfig().getDouble("nemesis.scaling.tierMultipliers." + normalized, 1.0));
    }

    private String formatCaptainName(CaptainRecord record) {
        String tier = record.progression() == null ? "COMMON" : record.progression().tier();
        String baseName = record.naming() == null ? "Nemesis Captain" : record.naming().displayName();
        int level = record.progression() == null ? 1 : record.progression().level();
        return "§c[" + tier + "] §6" + baseName + " §7(Lv." + level + ")";
    }

    private Location buildLocation(CaptainRecord.Origin origin) {
        World world = plugin.getServer().getWorld(origin.world());
        if (world == null) {
            return null;
        }
        return new Location(world, origin.spawnX(), origin.spawnY(), origin.spawnZ());
    }

    public LivingEntity resolveLiveKillerEntity(CaptainRecord record) {
        if (record == null || record.identity() == null || record.identity().captainId() == null) {
            return null;
        }

        UUID captainId = record.identity().captainId();
        LivingEntity cached = resolveCachedRuntimeEntity(record.state() == null ? null : record.state().runtimeEntityUuid(), captainId);
        if (cached != null) {
            return cached;
        }

        LivingEntity fromPdc = resolveByCaptainId(captainId);
        if (fromPdc != null) {
            return fromPdc;
        }

        handleMissingRuntimeBinding(record);
        return null;
    }

    private LivingEntity resolveCachedRuntimeEntity(UUID entityUuid, UUID captainId) {
        if (entityUuid == null) {
            return null;
        }
        Entity entity = plugin.getServer().getEntity(entityUuid);
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isValid()) {
            return null;
        }
        String storedCaptain = livingEntity.getPersistentDataContainer().get(captainIdKey, PersistentDataType.STRING);
        if (storedCaptain == null || !storedCaptain.equals(captainId.toString())) {
            return null;
        }
        return livingEntity;
    }

    private LivingEntity resolveByCaptainId(UUID captainId) {
        String expected = captainId.toString();
        for (World world : plugin.getServer().getWorlds()) {
            for (LivingEntity livingEntity : world.getLivingEntities()) {
                if (!livingEntity.isValid()) {
                    continue;
                }
                String storedCaptain = livingEntity.getPersistentDataContainer().get(captainIdKey, PersistentDataType.STRING);
                if (expected.equals(storedCaptain)) {
                    return livingEntity;
                }
            }
        }
        return null;
    }

    private void handleMissingRuntimeBinding(CaptainRecord record) {
        if (record.state() == null || record.state().state() != CaptainState.ACTIVE) {
            return;
        }

        transitionToDormantOrCooldown(record);
    }

    private void transitionToDormantOrCooldown(CaptainRecord record) {
        if (record == null || record.state() == null || record.state().state() != CaptainState.ACTIVE) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownMs = Math.max(0L, plugin.getConfig().getLong("nemesis.lifecycle.missingEntityCooldownMs", 60000L));
        CaptainRecord.State nextState = cooldownMs > 0
                ? stateMachine.onEscapeOrDespawn(now, now + cooldownMs)
                : stateMachine.onCooldownElapsed(now);

        CaptainRecord.State clearedState = new CaptainRecord.State(
                nextState.state(),
                nextState.cooldownUntilEpochMs(),
                nextState.lastSeenEpochMs(),
                null
        );

        captainRegistry.upsert(record.copyCore(
                record.identity(),
                record.origin(),
                record.victims(),
                record.nemesisScores(),
                record.progression(),
                record.naming(),
                record.traits(),
                record.minionPack(),
                clearedState,
                record.telemetry()
        ));
    }

    private boolean isChunkLoaded(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        return location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }



    public void setTraitService(CaptainTraitService traitService) {
        this.traitService = traitService;
    }

    public NamespacedKey getCaptainIdKey() {
        return captainIdKey;
    }

    public CaptainRegistry getCaptainRegistry() {
        return captainRegistry;
    }
    private EntityType resolveEntityType(CaptainRecord record) {
        if (record.naming() != null && record.naming().aliasSeed() != null) {
            String candidate = record.naming().aliasSeed().toUpperCase(Locale.ROOT);
            try {
                return EntityType.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // fall through
            }
        }

        ConfigurationSection eligible = plugin.getConfig().getConfigurationSection("nemesis.eligibleMobTypes");
        if (eligible != null) {
            for (String key : eligible.getKeys(false)) {
                if (!eligible.getBoolean(key, false)) {
                    continue;
                }
                try {
                    return EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    // continue
                }
            }
        }
        return EntityType.ZOMBIE;
    }
}
