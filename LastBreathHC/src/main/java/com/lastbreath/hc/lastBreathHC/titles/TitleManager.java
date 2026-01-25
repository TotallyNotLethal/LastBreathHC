package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Comparator;
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
    private static final Map<Title, List<String>> TITLE_EFFECTS = Map.ofEntries(
            Map.entry(Title.WANDERER, List.of("Custom: No bonus")),
            Map.entry(Title.THE_FALLEN, List.of("Custom: Armor +2")),
            Map.entry(Title.REVIVED, List.of("Custom: Max health +2")),
            Map.entry(Title.SOUL_RECLAIMER, List.of("Custom: Luck +1")),
            Map.entry(Title.ASTEROID_HUNTER, List.of("Custom: Attack speed +5%")),
            Map.entry(Title.RELIC_SEEKER, List.of("Custom: Luck +1")),
            Map.entry(Title.STAR_FORGER, List.of("Custom: Armor toughness +1")),
            Map.entry(Title.IRON_WILL, List.of("Custom: Armor +3")),
            Map.entry(Title.VOID_WALKER, List.of("Custom: Knockback resistance +5%")),
            Map.entry(Title.DEATH_DEFIER, List.of("Custom: Max health +2")),
            Map.entry(Title.TIME_TOUCHED, List.of("Custom: Movement speed +4%")),
            Map.entry(Title.LAST_SURVIVOR, List.of("Custom: Attack damage +1")),
            Map.entry(Title.MONSTER_HUNTER, List.of("Custom: Attack speed +5%")),
            Map.entry(Title.SOUL_REAPER, List.of("Custom: Max health +2")),
            Map.entry(Title.DOOMBRINGER, List.of("Custom: Max health +4")),
            Map.entry(Title.TRAILBLAZER, List.of("Custom: Movement speed +3%")),
            Map.entry(Title.HARVESTER, List.of("Custom: Max health +2")),
            Map.entry(Title.DEEP_DELVER, List.of("Custom: Armor toughness +1")),
            Map.entry(Title.PROSPECTOR, List.of("Custom: Luck +1")),
            Map.entry(Title.ANGLER, List.of("Custom: Movement speed +2%")),
            Map.entry(Title.SKYBOUND, List.of("Custom: Knockback resistance +5%")),
            Map.entry(Title.STARFORGED, List.of("Custom: Armor +2")),
            Map.entry(Title.AGELESS, List.of("Custom: Attack damage +0.5"))
    );
    private static final Map<Title, List<AttributeModifierSpec>> TITLE_ATTRIBUTE_MODIFIERS = Map.ofEntries(
            Map.entry(Title.THE_FALLEN, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("0f35d15c-b287-42b9-9ae3-e92d5e3d67f6"),
                            "title_the_fallen_armor", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.REVIVED, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("36d1f48b-5022-4c52-b5b9-6b35b25b095b"),
                            "title_revived_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.SOUL_RECLAIMER, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("b7ce4a7c-0ec4-4a87-94de-9ab4a28f6d43"),
                            "title_soul_reclaimer_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ASTEROID_HUNTER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("4a01a92f-bc0a-4d08-a2b4-700bb7d52f21"),
                            "title_asteroid_hunter_speed", 0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.RELIC_SEEKER, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("9cb6cc87-f56e-4a25-9b9b-0c9e3ca5ee32"),
                            "title_relic_seeker_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.STAR_FORGER, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("f5aa8577-8c8b-4c5b-8f49-35b4c6b2c3ac"),
                            "title_star_forger_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.IRON_WILL, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("6d5bc46e-50d7-4b2b-a76c-7fef3e804494"),
                            "title_iron_will_armor", 3.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.VOID_WALKER, List.of(
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("3b58b11b-3b06-41b0-842a-69f8512c15bd"),
                            "title_void_walker_knockback", 0.05, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DEATH_DEFIER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("eb01c1ba-6759-4b7a-bc62-716100462095"),
                            "title_death_defier_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.TIME_TOUCHED, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("62f20387-a392-4b92-8c53-06fd2b9ea36f"),
                            "title_time_touched_speed", 0.04, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.LAST_SURVIVOR, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("f092d2dd-36b4-4ac5-88db-2f4f48bd32d4"),
                            "title_last_survivor_damage", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.MONSTER_HUNTER, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_SPEED, UUID.fromString("3a5a805c-8231-4b54-9d4d-9a399e1c1bc2"),
                            "title_monster_hunter_speed", 0.05, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.SOUL_REAPER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("9c196f0f-81a5-4d4b-8825-0e8c2ed6895c"),
                            "title_soul_reaper_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DOOMBRINGER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("96f15874-e8f8-4a8d-a4ac-244c5df1cdd4"),
                            "title_doombringer_health", 4.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.TRAILBLAZER, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("9fcae80c-5a47-4dff-93a1-17c5317adf7b"),
                            "title_trailblazer_speed", 0.03, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.HARVESTER, List.of(
                    new AttributeModifierSpec(Attribute.MAX_HEALTH, UUID.fromString("a5f79a7b-7d0b-4da4-a95a-6a48f3b3b2c2"),
                            "title_harvester_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DEEP_DELVER, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR_TOUGHNESS, UUID.fromString("2d7b1e25-6a07-4a7c-8a2b-5932b5030f5b"),
                            "title_deep_delver_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.PROSPECTOR, List.of(
                    new AttributeModifierSpec(Attribute.LUCK, UUID.fromString("1c0dbb24-24f5-40d9-93f3-2c7d4f7b23f7"),
                            "title_prospector_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ANGLER, List.of(
                    new AttributeModifierSpec(Attribute.MOVEMENT_SPEED, UUID.fromString("6e5b2f1a-9b39-4f6b-8a48-0f6a4e7a6f9b"),
                            "title_angler_speed", 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.SKYBOUND, List.of(
                    new AttributeModifierSpec(Attribute.KNOCKBACK_RESISTANCE, UUID.fromString("9a2c9a31-5f6a-42e3-9a27-2d9ce7f3a9e0"),
                            "title_skybound_knockback", 0.05, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.STARFORGED, List.of(
                    new AttributeModifierSpec(Attribute.ARMOR, UUID.fromString("a19d43b9-3d08-43ce-9c74-7c03bfa6c16e"),
                            "title_starforged_armor", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.AGELESS, List.of(
                    new AttributeModifierSpec(Attribute.ATTACK_DAMAGE, UUID.fromString("fb47bcb6-1d1b-4ff8-9b6e-32a9d2a44273"),
                            "title_ageless_damage", 0.5, AttributeModifier.Operation.ADD_NUMBER)
            ))
    );
    private static final Set<UUID> TITLE_MODIFIER_IDS = collectModifierIds();

    private TitleManager() {
    }

    public static void initialize(PlayerStats stats) {
        if (stats.unlockedTitles.isEmpty()) {
            stats.unlockedTitles.add(DEFAULT_TITLE);
        }
        if (stats.equippedTitle == null) {
            stats.equippedTitle = DEFAULT_TITLE;
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
    }

    public static boolean equipTitle(Player player, Title title) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        initialize(stats);
        if (!stats.unlockedTitles.contains(title)) {
            return false;
        }
        stats.equippedTitle = title;
        applyEquippedTitleEffects(player, title);
        refreshPlayerTabTitle(player);
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
        long seconds = stats.timeAlive / 20;
        if (seconds >= ONE_HOUR_SECONDS) {
            unlockTitle(player, Title.TIME_TOUCHED, "Survived for at least one hour.");
        }
        if (seconds >= TEN_HOURS_SECONDS) {
            unlockTitle(player, Title.IRON_WILL, "Survived for at least 10 hours.");
        }
        if (seconds >= TWENTY_FIVE_HOURS_SECONDS) {
            unlockTitle(player, Title.VOID_WALKER, "Survived for at least 25 hours.");
        }
        if (seconds >= ONE_HUNDRED_HOURS_SECONDS) {
            unlockTitle(player, Title.LAST_SURVIVOR, "Survived for at least 100 hours.");
        }
        if (seconds >= AGELESS_SECONDS) {
            unlockTitle(player, Title.AGELESS, "Survived for at least 250 hours.");
        }
    }

    public static void checkProgressTitles(Player player) {
        PlayerStats stats = StatsManager.get(player.getUniqueId());
        if (stats.mobsKilled >= MONSTER_HUNTER_KILLS) {
            unlockTitle(player, Title.MONSTER_HUNTER, "Slayed " + MONSTER_HUNTER_KILLS + " mobs.");
        }
        if (stats.mobsKilled >= SOUL_REAPER_KILLS) {
            unlockTitle(player, Title.SOUL_REAPER, "Slayed " + SOUL_REAPER_KILLS + " mobs.");
        }
        if (stats.mobsKilled >= DOOMBRINGER_KILLS) {
            unlockTitle(player, Title.DOOMBRINGER, "Slayed " + DOOMBRINGER_KILLS + " mobs.");
        }
        long travelDistance = getTravelDistanceCm(player);
        if (travelDistance >= TRAILBLAZER_DISTANCE_CM) {
            unlockTitle(player, Title.TRAILBLAZER, "Traveled over " + (TRAILBLAZER_DISTANCE_CM / 100_000) + " km.");
        }
        if (stats.cropsHarvested >= HARVESTER_CROPS) {
            unlockTitle(player, Title.HARVESTER, "Harvested " + HARVESTER_CROPS + " crops.");
        }
        if (stats.blocksMined >= DEEP_DELVER_BLOCKS) {
            unlockTitle(player, Title.DEEP_DELVER, "Mined " + DEEP_DELVER_BLOCKS + " deep blocks.");
        }
        if (stats.rareOresMined >= PROSPECTOR_RARES) {
            unlockTitle(player, Title.PROSPECTOR, "Unearthed " + PROSPECTOR_RARES + " rare ores.");
        }
        if (player.getStatistic(Statistic.FISH_CAUGHT) >= ANGLER_FISH) {
            unlockTitle(player, Title.ANGLER, "Caught " + ANGLER_FISH + " fish.");
        }
        if (player.getStatistic(Statistic.AVIATE_ONE_CM) >= SKYBOUND_DISTANCE_CM) {
            unlockTitle(player, Title.SKYBOUND, "Glided over " + (SKYBOUND_DISTANCE_CM / 100_000) + " km.");
        }
        if (stats.asteroidLoots >= STARFORGED_ASTEROIDS) {
            unlockTitle(player, Title.STARFORGED, "Mastered " + STARFORGED_ASTEROIDS + " asteroid raids.");
        }
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
        player.setPlayerListName(getTitleTabTag(player) + player.getName());
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
