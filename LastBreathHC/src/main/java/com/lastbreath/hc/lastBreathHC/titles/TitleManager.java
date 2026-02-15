package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class TitleManager {

    private static final Title DEFAULT_TITLE = Title.WANDERER;
    private static final long ONE_HOUR_SECONDS = 60 * 60;
    private static final long TEN_HOURS_SECONDS = 10 * 60 * 60;
    private static final long TWENTY_FIVE_HOURS_SECONDS = 25 * 60 * 60;
    private static final long ONE_HUNDRED_HOURS_SECONDS = 100 * 60 * 60;
    private static final int TITLE_EFFECT_REFRESH_TICKS = 4 * 20;
    private static final int MONSTER_HUNTER_KILLS = 250;
    private static final int SOUL_REAPER_KILLS = 1000;
    private static final int DOOMBRINGER_KILLS = 5000;
    private static final long TRAILBLAZER_DISTANCE_CM = 10_000_000L;
    private static final int HARVESTER_CROPS = 500;
    private static final int DEEP_DELVER_BLOCKS = 3000;
    private static final int PROSPECTOR_RARES = 100;
    private static final int ANGLER_FISH = 200;
    private static final long SKYBOUND_DISTANCE_CM = 2_500_000L;
    private static final int STARFORGED_ASTEROIDS = 500;
    private static final long AGELESS_SECONDS = 250 * 60 * 60;
    private static final Set<Title> WORLD_SCALER_REQUIRED_BOSS_TITLES = EnumSet.of(
            Title.GRAVEWARDEN_BANE,
            Title.STORM_HERALD,
            Title.HOLLOW_COLOSSUS,
            Title.ASHEN_ORACLE
    );
    private static final Map<Title, List<String>> TITLE_EFFECTS = Map.ofEntries(
            Map.entry(Title.WANDERER, List.of("Custom: No bonus")),
            Map.entry(Title.THE_FALLEN, List.of("Custom: Armor +1", "Custom: Knockback resistance +3%")),
            Map.entry(Title.REVIVED, List.of("Custom: Max health +1")),
            Map.entry(Title.SOUL_RECLAIMER, List.of("Custom: Luck +0.5")),
            Map.entry(Title.ASTEROID_HUNTER, List.of("Custom: Attack speed +4%")),
            Map.entry(Title.RELIC_SEEKER, List.of("Custom: Luck +1", "Custom: Movement speed +1%")),
            Map.entry(Title.STAR_FORGER, List.of("Custom: Armor toughness +1")),
            Map.entry(Title.IRON_WILL, List.of("Custom: Armor +2")),
            Map.entry(Title.VOID_WALKER, List.of("Custom: Knockback resistance +8%")),
            Map.entry(Title.DEATH_DEFIER, List.of("Custom: Max health +2", "Custom: Armor +1")),
            Map.entry(Title.TIME_TOUCHED, List.of("Custom: Movement speed +3%")),
            Map.entry(Title.LAST_SURVIVOR, List.of("Custom: Attack damage +0.75", "Custom: Armor toughness +1")),
            Map.entry(Title.MONSTER_HUNTER, List.of("Custom: Attack speed +6%")),
            Map.entry(Title.SOUL_REAPER, List.of("Custom: Attack damage +0.5", "Custom: Max health +1")),
            Map.entry(Title.DOOMBRINGER, List.of("Custom: Attack damage +1", "Custom: Max health +2")),
            Map.entry(Title.TRAILBLAZER, List.of("Custom: Movement speed +4%")),
            Map.entry(Title.HARVESTER, List.of("Custom: Max health +1", "Custom: Luck +0.5")),
            Map.entry(Title.DEEP_DELVER, List.of("Custom: Armor +1", "Custom: Armor toughness +1")),
            Map.entry(Title.PROSPECTOR, List.of("Custom: Luck +1", "Custom: Armor +1")),
            Map.entry(Title.ANGLER, List.of("Custom: Movement speed +2%", "Custom: Luck +0.5")),
            Map.entry(Title.SKYBOUND, List.of("Custom: Movement speed +2%", "Custom: Knockback resistance +6%")),
            Map.entry(Title.STARFORGED, List.of("Custom: Armor +2", "Custom: Attack speed +4%")),
            Map.entry(Title.AGELESS, List.of("Custom: Max health +2", "Custom: Movement speed +2%")),
            Map.entry(Title.GRAVEWARDEN_BANE, List.of("Custom: Max health +3", "Custom: Armor +2", "Custom: Boss landing aura")),
            Map.entry(Title.STORM_HERALD, List.of("Custom: Movement speed +5%", "Custom: Attack speed +5%", "Custom: Boss landing aura")),
            Map.entry(Title.HOLLOW_COLOSSUS, List.of("Custom: Armor +3", "Custom: Knockback resistance +10%", "Custom: Boss landing aura")),
            Map.entry(Title.ASHEN_ORACLE, List.of("Custom: Attack damage +1.25", "Custom: Armor toughness +1", "Custom: Boss landing aura")),
            Map.entry(Title.WORLD_SCALER, List.of("Custom: Background toggle", "Custom: Deal world scaling multiplier damage to scaled mobs"))
    );
    private static final Map<Title, List<AttributeModifierSpec>> TITLE_ATTRIBUTE_MODIFIERS = Map.ofEntries(
            Map.entry(Title.THE_FALLEN, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("0f35d15c-b287-42b9-9ae3-e92d5e3d67f6"),
                            "title_the_fallen_armor", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("c51635fc-0f3f-43f5-9195-72f2c2b89e74"),
                            "title_the_fallen_kb", 0.03, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.REVIVED, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("36d1f48b-5022-4c52-b5b9-6b35b25b095b"),
                            "title_revived_health", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.SOUL_RECLAIMER, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("b7ce4a7c-0ec4-4a87-94de-9ab4a28f6d43"),
                            "title_soul_reclaimer_luck", 0.5, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ASTEROID_HUNTER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("4a01a92f-bc0a-4d08-a2b4-700bb7d52f21"),
                            "title_asteroid_hunter_speed", 0.04, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.RELIC_SEEKER, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("9cb6cc87-f56e-4a25-9b9b-0c9e3ca5ee32"),
                            "title_relic_seeker_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("dc4d8f35-f1e0-4b6e-b746-1320e6f396f7"),
                            "title_relic_seeker_speed", 0.01, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.STAR_FORGER, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("f5aa8577-8c8b-4c5b-8f49-35b4c6b2c3ac"),
                            "title_star_forger_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.IRON_WILL, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("6d5bc46e-50d7-4b2b-a76c-7fef3e804494"),
                            "title_iron_will_armor", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.VOID_WALKER, List.of(
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("3b58b11b-3b06-41b0-842a-69f8512c15bd"),
                            "title_void_walker_knockback", 0.08, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DEATH_DEFIER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("eb01c1ba-6759-4b7a-bc62-716100462095"),
                            "title_death_defier_health", 2.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("f5204ea4-cb59-4962-9cb3-2f6f4e0dbf7d"),
                            "title_death_defier_armor", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.TIME_TOUCHED, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("62f20387-a392-4b92-8c53-06fd2b9ea36f"),
                            "title_time_touched_speed", 0.03, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.LAST_SURVIVOR, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("f092d2dd-36b4-4ac5-88db-2f4f48bd32d4"),
                            "title_last_survivor_damage", 0.75, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("0364ded3-d9f7-4905-9030-71a584d30284"),
                            "title_last_survivor_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.MONSTER_HUNTER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("3a5a805c-8231-4b54-9d4d-9a399e1c1bc2"),
                            "title_monster_hunter_speed", 0.06, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.SOUL_REAPER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("7b90daa2-4957-49be-b0ce-a5f532a8e5de"),
                            "title_soul_reaper_damage", 0.5, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("9c196f0f-81a5-4d4b-8825-0e8c2ed6895c"),
                            "title_soul_reaper_health", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DOOMBRINGER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("2f4959dc-adc3-44aa-a6f2-93244f4f0a38"),
                            "title_doombringer_damage", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("96f15874-e8f8-4a8d-a4ac-244c5df1cdd4"),
                            "title_doombringer_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.TRAILBLAZER, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("9fcae80c-5a47-4dff-93a1-17c5317adf7b"),
                            "title_trailblazer_speed", 0.04, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.HARVESTER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("a5f79a7b-7d0b-4da4-a95a-6a48f3b3b2c2"),
                            "title_harvester_health", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("f8209e65-3a30-40ee-a571-f1f52ec3b3ce"),
                            "title_harvester_luck", 0.5, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DEEP_DELVER, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("b28d1f0a-e26e-4b6d-a72b-839930cf628f"),
                            "title_deep_delver_armor", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("2d7b1e25-6a07-4a7c-8a2b-5932b5030f5b"),
                            "title_deep_delver_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.PROSPECTOR, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("1c0dbb24-24f5-40d9-93f3-2c7d4f7b23f7"),
                            "title_prospector_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("1f42e3df-4f15-4d5c-b52a-ec9b768d9415"),
                            "title_prospector_armor", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ANGLER, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("6e5b2f1a-9b39-4f6b-8a48-0f6a4e7a6f9b"),
                            "title_angler_speed", 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("c32a3a02-5444-40a4-bec3-45468918cfd0"),
                            "title_angler_luck", 0.5, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.SKYBOUND, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("2d667255-7d17-48aa-8329-9450f7a1c3f7"),
                            "title_skybound_speed", 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("9a2c9a31-5f6a-42e3-9a27-2d9ce7f3a9e0"),
                            "title_skybound_knockback", 0.06, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.STARFORGED, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("a19d43b9-3d08-43ce-9c74-7c03bfa6c16e"),
                            "title_starforged_armor", 2.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("d2726d73-b5e7-4fdf-a733-b4b2ea863804"),
                            "title_starforged_speed", 0.04, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.AGELESS, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("12ce67a4-7795-4f50-8ec5-c00155b4f247"),
                            "title_ageless_health", 2.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("dcf8cb9c-e9f7-4f88-97fe-bf3ef2ee2d89"),
                            "title_ageless_speed", 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.GRAVEWARDEN_BANE, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("e4cbf3c7-4a69-4f6b-8d5a-0ea72df99c2b"),
                            "title_gravewarden_bane_health", 3.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("3b9fdbe5-ec8f-4e26-aac0-31f5f2d2f15f"),
                            "title_gravewarden_bane_armor", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.STORM_HERALD, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("7d9dc40b-3a38-4c49-b9cd-6e1b280f9a4c"),
                            "title_storm_herald_speed", 0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1),
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("e04fd1c4-c488-43da-813d-6746fc76df9b"),
                            "title_storm_herald_attack_speed", 0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.HOLLOW_COLOSSUS, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("fd3e8afc-6ef0-4f0c-8a35-1d0fef4e7f82"),
                            "title_hollow_colossus_armor", 3.0, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("ef8f3650-e02c-4a9a-b4c0-38ef6528a1e1"),
                            "title_hollow_colossus_knockback", 0.10, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ASHEN_ORACLE, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("b2b31c2a-2b85-4f4b-a24c-3d3f1311a792"),
                            "title_ashen_oracle_damage", 1.25, AttributeModifier.Operation.ADD_NUMBER),
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("b54b839b-c0d8-4fe2-ac76-375bbf6be4db"),
                            "title_ashen_oracle_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            ))
    );
    private static final Map<Title, BossAura> BOSS_TITLE_AURAS = Map.of(
            Title.GRAVEWARDEN_BANE, BossAura.SOUL_FLAME,
            Title.STORM_HERALD, BossAura.STORM_SPARK,
            Title.HOLLOW_COLOSSUS, BossAura.DUSTVEIL,
            Title.ASHEN_ORACLE, BossAura.CINDER_VEIL
    );
    private static final Set<UUID> TITLE_MODIFIER_IDS = collectModifierIds();

    private TitleManager() {
    }

    public static void initialize(PlayerStats stats) {
        if (stats.unlockedTitles.isEmpty()) {
            stats.unlockedTitles.add(DEFAULT_TITLE);
        }
        if (stats.equippedTitle == null || isBackgroundOnlyTitle(stats.equippedTitle)) {
            stats.equippedTitle = DEFAULT_TITLE;
        }
        ensureWorldScalerUnlocked(stats);
        if (!stats.unlockedTitles.contains(Title.WORLD_SCALER)) {
            stats.worldScalerEnabled = false;
        }
    }

    public static void unlockTitle(Player player, Title title, String reason) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (stats.unlockedTitles.add(title)) {
            if (stats.equippedTitle == null) {
                stats.equippedTitle = title;
                applyEquippedTitleEffects(player, title);
                refreshPlayerTabTitle(player);
            }
            player.sendMessage("§6New title unlocked: §e" + title.displayName()
                    + "§6. " + reason);
        }
        if (ensureWorldScalerUnlocked(stats)) {
            player.sendMessage("§6New background title unlocked: §e" + Title.WORLD_SCALER.displayName()
                    + "§6. " + Title.WORLD_SCALER.requirementDescription());
        }
    }

    public static boolean equipTitle(Player player, Title title) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (!stats.unlockedTitles.contains(title) || isBackgroundOnlyTitle(title)) {
            return false;
        }
        stats.equippedTitle = title;
        applyEquippedTitleEffects(player, title);
        refreshPlayerTabTitle(player);
        return true;
    }

    public static boolean isBackgroundOnlyTitle(Title title) {
        return title == Title.WORLD_SCALER;
    }

    public static boolean isWorldScalerEnabled(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        return stats.worldScalerEnabled && stats.unlockedTitles.contains(Title.WORLD_SCALER);
    }

    public static boolean setWorldScalerEnabled(Player player, boolean enabled) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (!stats.unlockedTitles.contains(Title.WORLD_SCALER)) {
            return false;
        }
        stats.worldScalerEnabled = enabled;
        StatsManager.markDirty(player.getUniqueId());
        return true;
    }

    public static String getTitleTag(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (stats.equippedTitle == null) {
            return "";
        }
        return "§7[" + stats.equippedTitle.displayName() + "§7] ";
    }

    public static String getTitleTabTag(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (stats.equippedTitle == null) {
            return "";
        }
        return "§7[" + stats.equippedTitle.tabTag() + "§7] ";
    }

    public static Title getEquippedTitle(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        return stats.equippedTitle;
    }

    public static BossAura getBossTitleAura(Title title) {
        if (title == null) {
            return null;
        }
        return BOSS_TITLE_AURAS.get(title);
    }

    public static List<String> getTitleEffects(Title title) {
        if (title == null) {
            return List.of();
        }
        return TITLE_EFFECTS.getOrDefault(title, List.of());
    }

    public static String formatTitleList(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        return stats.unlockedTitles.stream()
                .sorted(Comparator.comparing(Title::displayName))
                .map(title -> {
                    if (title == stats.equippedTitle) {
                        return "§a" + title.displayName() + " §7(Equipped)";
                    }
                    return "§f" + title.displayName();
                })
                .collect(Collectors.joining("§7, "));
    }

    public static List<String> getUnlockedTitleInputs(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        return stats.unlockedTitles.stream()
                .sorted(Comparator.comparing(Title::displayName))
                .map(Title::displayName)
                .toList();
    }

    public static List<Title> getUnlockedTitles(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        return stats.unlockedTitles.stream()
                .sorted(Comparator.comparing(Title::displayName))
                .toList();
    }

    public static void checkTimeBasedTitles(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        stats.timeAlive = player.getStatistic(Statistic.PLAY_ONE_MINUTE);
        StatsManager.markDirty(player.getUniqueId());
        long seconds = stats.timeAlive / 20;
        if (seconds >= ONE_HOUR_SECONDS) {
            unlockTitle(player, Title.TIME_TOUCHED, Title.TIME_TOUCHED.requirementDescription());
        }
        if (seconds >= TEN_HOURS_SECONDS) {
            unlockTitle(player, Title.IRON_WILL, Title.IRON_WILL.requirementDescription());
        }
        if (seconds >= TWENTY_FIVE_HOURS_SECONDS) {
            unlockTitle(player, Title.VOID_WALKER, Title.VOID_WALKER.requirementDescription());
        }
        if (seconds >= ONE_HUNDRED_HOURS_SECONDS) {
            unlockTitle(player, Title.LAST_SURVIVOR, Title.LAST_SURVIVOR.requirementDescription());
        }
        if (seconds >= AGELESS_SECONDS) {
            unlockTitle(player, Title.AGELESS, Title.AGELESS.requirementDescription());
        }
    }

    public static void checkProgressTitles(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.mobsKilled >= MONSTER_HUNTER_KILLS) {
            unlockTitle(player, Title.MONSTER_HUNTER, Title.MONSTER_HUNTER.requirementDescription());
        }
        if (stats.mobsKilled >= SOUL_REAPER_KILLS) {
            unlockTitle(player, Title.SOUL_REAPER, Title.SOUL_REAPER.requirementDescription());
        }
        if (stats.mobsKilled >= DOOMBRINGER_KILLS) {
            unlockTitle(player, Title.DOOMBRINGER, Title.DOOMBRINGER.requirementDescription());
        }
        long travelDistance = getTravelDistanceCm(player);
        if (travelDistance >= TRAILBLAZER_DISTANCE_CM) {
            unlockTitle(player, Title.TRAILBLAZER, Title.TRAILBLAZER.requirementDescription());
        }
        if (stats.cropsHarvested >= HARVESTER_CROPS) {
            unlockTitle(player, Title.HARVESTER, Title.HARVESTER.requirementDescription());
        }
        if (stats.blocksMined >= DEEP_DELVER_BLOCKS) {
            unlockTitle(player, Title.DEEP_DELVER, Title.DEEP_DELVER.requirementDescription());
        }
        if (stats.rareOresMined >= PROSPECTOR_RARES) {
            unlockTitle(player, Title.PROSPECTOR, Title.PROSPECTOR.requirementDescription());
        }
        if (player.getStatistic(Statistic.FISH_CAUGHT) >= ANGLER_FISH) {
            unlockTitle(player, Title.ANGLER, Title.ANGLER.requirementDescription());
        }
        if (player.getStatistic(Statistic.AVIATE_ONE_CM) >= SKYBOUND_DISTANCE_CM) {
            unlockTitle(player, Title.SKYBOUND, Title.SKYBOUND.requirementDescription());
        }
        if (stats.asteroidLoots >= STARFORGED_ASTEROIDS) {
            unlockTitle(player, Title.STARFORGED, Title.STARFORGED.requirementDescription());
        }
    }

    private static boolean ensureWorldScalerUnlocked(PlayerStats stats) {
        if (stats == null || stats.unlockedTitles.contains(Title.WORLD_SCALER)) {
            return false;
        }
        if (!stats.unlockedTitles.containsAll(WORLD_SCALER_REQUIRED_BOSS_TITLES)) {
            return false;
        }
        stats.unlockedTitles.add(Title.WORLD_SCALER);
        StatsManager.markDirty(stats.uuid);
        return true;
    }

    public static void applyEquippedTitleEffects(Player player, Title title) {
        if (player == null || title == null) {
            return;
        }
        clearTitleAttributeModifiers(player);
        applyTitleAttributeModifiers(player, title);
    }

    private static long getTravelDistanceCm(Player player) {
        return (long) player.getStatistic(Statistic.WALK_ONE_CM)
                + player.getStatistic(Statistic.SPRINT_ONE_CM)
                + player.getStatistic(Statistic.SWIM_ONE_CM)
                + player.getStatistic(Statistic.FLY_ONE_CM)
                + player.getStatistic(Statistic.AVIATE_ONE_CM)
                + player.getStatistic(Statistic.BOAT_ONE_CM)
                + player.getStatistic(Statistic.MINECART_ONE_CM)
                + player.getStatistic(Statistic.HORSE_ONE_CM);
    }

    private static void applyTitleAttributeModifiers(Player player, Title title) {
        List<AttributeModifierSpec> modifiers = TITLE_ATTRIBUTE_MODIFIERS.get(title);
        if (modifiers == null) {
            return;
        }
        for (AttributeModifierSpec modifier : modifiers) {
            AttributeInstance instance = player.getAttribute(modifier.attribute());
            if (instance == null) {
                continue;
            }
            removeExistingTitleModifier(instance, modifier.uuid());
            instance.addModifier(modifier.asModifier());
        }
    }

    private static void clearTitleAttributeModifiers(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            removeExistingTitleModifiers(instance);
        }
    }

    private static void removeExistingTitleModifiers(AttributeInstance instance) {
        for (AttributeModifier modifier : instance.getModifiers()) {
            if (TITLE_MODIFIER_IDS.contains(modifier.getUniqueId())) {
                instance.removeModifier(modifier);
            }
        }
    }

    private static void removeExistingTitleModifier(AttributeInstance instance, UUID modifierId) {
        for (AttributeModifier existing : instance.getModifiers()) {
            if (existing.getUniqueId().equals(modifierId)) {
                instance.removeModifier(existing);
                return;
            }
        }
    }

    private static Set<UUID> collectModifierIds() {
        Set<UUID> ids = new HashSet<>();
        for (List<AttributeModifierSpec> modifiers : TITLE_ATTRIBUTE_MODIFIERS.values()) {
            for (AttributeModifierSpec modifier : modifiers) {
                ids.add(modifier.uuid());
            }
        }
        return ids;
    }

    private record AttributeModifierSpec(
            Attribute attribute,
            UUID uuid,
            String name,
            double amount,
            AttributeModifier.Operation operation
    ) {
        AttributeModifier asModifier() {
            return new AttributeModifier(uuid, name, amount, operation);
        }
    }

    public static void refreshPlayerTabTitle(Player player) {
        if (player == null) {
            return;
        }
        NamespacedKey nicknameKey = new NamespacedKey(LastBreathHC.getInstance(), "nickname");
        String nickname = player.getPersistentDataContainer().get(nicknameKey, PersistentDataType.STRING);
        String displayName = nickname == null || nickname.isBlank() ? player.getName() : nickname;
        player.setPlayerListName(getTitleTabTag(player) + displayName);
    }

    public static void refreshAllTabTitles() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            refreshPlayerTabTitle(online);
        }
    }

    public static void refreshEquippedTitleEffects() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            applyEquippedTitleEffects(online, getEquippedTitle(online));
        }
    }

    public static int getTitleEffectRefreshTicks() {
        return TITLE_EFFECT_REFRESH_TICKS;
    }
}
