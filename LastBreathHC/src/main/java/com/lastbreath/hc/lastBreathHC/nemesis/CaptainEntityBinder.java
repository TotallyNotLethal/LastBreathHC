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
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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

    public CaptainEntityBinder(LastBreathHC plugin, CaptainRegistry captainRegistry) {
        this.plugin = plugin;
        this.captainRegistry = captainRegistry;
        this.captainIdKey = new NamespacedKey(plugin, "nemesis_captain_uuid");

        this.baseHealth = Math.max(1.0, plugin.getConfig().getDouble("nemesis.scaling.baseHealth", 30.0));
        this.healthPerLevel = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scaling.healthPerLevel", 5.0));
        this.damagePerLevel = Math.max(0.0, plugin.getConfig().getDouble("nemesis.scaling.damagePerLevel", 1.0));
    }

    public Optional<LivingEntity> spawnOrBind(CaptainRecord record) {
        if (record == null || record.identity() == null || record.origin() == null) {
            return Optional.empty();
        }

        LivingEntity upgradeCandidate = resolveLiveKillerEntity(record);
        if (upgradeCandidate != null) {
            bind(upgradeCandidate, record);
            return Optional.of(upgradeCandidate);
        }

        Location spawnLocation = buildLocation(record.origin());
        if (spawnLocation == null || spawnLocation.getWorld() == null) {
            return Optional.empty();
        }

        if (!spawnLocation.getChunk().isLoaded() && !spawnLocation.getChunk().load()) {
            return Optional.empty();
        }

        Entity entity = spawnLocation.getWorld().spawnEntity(spawnLocation, resolveEntityType(record));
        if (!(entity instanceof LivingEntity livingEntity)) {
            entity.remove();
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
        bind(live, record);
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
        if (entity == null || record == null || record.identity() == null || record.identity().captainId() == null) {
            return;
        }

        entity.addScoreboardTag(CAPTAIN_SCOREBOARD_TAG);
        entity.getPersistentDataContainer().set(captainIdKey, PersistentDataType.STRING, record.identity().captainId().toString());

        String customName = formatCaptainName(record);
        entity.customName(net.kyori.adventure.text.Component.text(customName));
        entity.setCustomNameVisible(true);

        applyAttributes(entity, record);
        applyEquipmentRolls(entity, record);
        applyTraitHooks(entity, record);
        if (traitService != null) {
            traitService.applyOnBind(entity, record);
        }
    }

    private void applyAttributes(LivingEntity entity, CaptainRecord record) {
        int level = Math.max(1, record.progression() == null ? 1 : record.progression().level());
        double tierMultiplier = tierMultiplier(record.progression() == null ? "COMMON" : record.progression().tier());

        double targetHealth = Math.max(1.0, (baseHealth + (level - 1) * healthPerLevel) * tierMultiplier);
        AttributeInstance maxHealthAttribute = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealthAttribute != null) {
            maxHealthAttribute.setBaseValue(targetHealth);
            entity.setHealth(Math.min(targetHealth, targetHealth));
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

        double chance = plugin.getConfig().getDouble("nemesis.equipment.rollChance", 0.0);
        if (chance <= 0.0 || Math.random() > chance) {
            return;
        }

        EntityEquipment equipment = mob.getEquipment();
        if (equipment == null) {
            return;
        }

        ItemStack helmet = new ItemStack(org.bukkit.Material.IRON_HELMET);
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("Nemesis Crown");
            helmet.setItemMeta(meta);
        }
        equipment.setHelmet(helmet);
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

        captainRegistry.upsert(new CaptainRecord(
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
