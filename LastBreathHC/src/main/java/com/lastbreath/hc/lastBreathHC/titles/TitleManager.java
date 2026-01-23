package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
    private static final int TITLE_EFFECT_DURATION_TICKS = 8 * 20;
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
            Map.entry(Title.WANDERER, List.of("No special effect")),
            Map.entry(Title.THE_FALLEN, List.of("Brief resistance after respawn")),
            Map.entry(Title.REVIVED, List.of("Short regeneration after revive")),
            Map.entry(Title.SOUL_RECLAIMER, List.of("Minor luck pulse")),
            Map.entry(Title.ASTEROID_HUNTER, List.of("Short haste near asteroids")),
            Map.entry(Title.RELIC_SEEKER, List.of("Minor night vision pulse")),
            Map.entry(Title.STAR_FORGER, List.of("Short fire resistance pulse")),
            Map.entry(Title.IRON_WILL, List.of("Minor resistance pulse")),
            Map.entry(Title.VOID_WALKER, List.of("Minor slow falling pulse")),
            Map.entry(Title.DEATH_DEFIER, List.of("Minor absorption pulse")),
            Map.entry(Title.TIME_TOUCHED, List.of("Minor speed pulse")),
            Map.entry(Title.LAST_SURVIVOR, List.of("Minor strength pulse")),
            Map.entry(Title.MONSTER_HUNTER, List.of("Minor haste pulse")),
            Map.entry(Title.SOUL_REAPER, List.of("Minor saturation pulse")),
            Map.entry(Title.DOOMBRINGER, List.of("Minor health boost pulse")),
            Map.entry(Title.TRAILBLAZER, List.of("Minor jump boost pulse", "Custom: +3% movement speed")),
            Map.entry(Title.HARVESTER, List.of("Minor hero of the village pulse", "Custom: +2 max health")),
            Map.entry(Title.DEEP_DELVER, List.of("Minor haste pulse", "Custom: +1 armor toughness")),
            Map.entry(Title.PROSPECTOR, List.of("Minor luck pulse", "Custom: +1 luck")),
            Map.entry(Title.ANGLER, List.of("Minor water breathing pulse", "Custom: +2% movement speed")),
            Map.entry(Title.SKYBOUND, List.of("Minor slow falling pulse", "Custom: +5% knockback resistance")),
            Map.entry(Title.STARFORGED, List.of("Minor fire resistance pulse")),
            Map.entry(Title.AGELESS, List.of("Minor night vision pulse"))
    );
    private static final Map<Title, List<AttributeModifierSpec>> TITLE_ATTRIBUTE_MODIFIERS = Map.ofEntries(
            Map.entry(Title.TRAILBLAZER, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_MOVEMENT_SPEED, UUID.fromString("9fcae80c-5a47-4dff-93a1-17c5317adf7b"),
                            "title_trailblazer_speed", 0.03, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.HARVESTER, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_MAX_HEALTH, UUID.fromString("a5f79a7b-7d0b-4da4-a95a-6a48f3b3b2c2"),
                            "title_harvester_health", 2.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.DEEP_DELVER, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_ARMOR_TOUGHNESS, UUID.fromString("2d7b1e25-6a07-4a7c-8a2b-5932b5030f5b"),
                            "title_deep_delver_toughness", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.PROSPECTOR, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_LUCK, UUID.fromString("1c0dbb24-24f5-40d9-93f3-2c7d4f7b23f7"),
                            "title_prospector_luck", 1.0, AttributeModifier.Operation.ADD_NUMBER)
            )),
            Map.entry(Title.ANGLER, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_MOVEMENT_SPEED, UUID.fromString("6e5b2f1a-9b39-4f6b-8a48-0f6a4e7a6f9b"),
                            "title_angler_speed", 0.02, AttributeModifier.Operation.MULTIPLY_SCALAR_1)
            )),
            Map.entry(Title.SKYBOUND, List.of(
                    new AttributeModifierSpec(Attribute.GENERIC_KNOCKBACK_RESISTANCE, UUID.fromString("9a2c9a31-5f6a-42e3-9a27-2d9ce7f3a9e0"),
                            "title_skybound_knockback", 0.05, AttributeModifier.Operation.ADD_NUMBER)
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
        PotionEffect effect = switch (title) {
            case THE_FALLEN -> new PotionEffect(PotionEffectType.RESISTANCE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case REVIVED -> new PotionEffect(PotionEffectType.REGENERATION, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case SOUL_RECLAIMER -> new PotionEffect(PotionEffectType.LUCK, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case ASTEROID_HUNTER -> new PotionEffect(PotionEffectType.HASTE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case RELIC_SEEKER, AGELESS -> new PotionEffect(PotionEffectType.NIGHT_VISION, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case STAR_FORGER, STARFORGED -> new PotionEffect(PotionEffectType.FIRE_RESISTANCE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case IRON_WILL -> new PotionEffect(PotionEffectType.RESISTANCE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case VOID_WALKER -> new PotionEffect(PotionEffectType.SLOW_FALLING, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case DEATH_DEFIER -> new PotionEffect(PotionEffectType.ABSORPTION, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case TIME_TOUCHED -> new PotionEffect(PotionEffectType.SPEED, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case LAST_SURVIVOR -> new PotionEffect(PotionEffectType.STRENGTH, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case MONSTER_HUNTER -> new PotionEffect(PotionEffectType.HASTE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case SOUL_REAPER -> new PotionEffect(PotionEffectType.SATURATION, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case DOOMBRINGER -> new PotionEffect(PotionEffectType.HEALTH_BOOST, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case TRAILBLAZER -> new PotionEffect(PotionEffectType.JUMP, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case HARVESTER -> new PotionEffect(PotionEffectType.HERO_OF_THE_VILLAGE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case DEEP_DELVER -> new PotionEffect(PotionEffectType.HASTE, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case PROSPECTOR -> new PotionEffect(PotionEffectType.LUCK, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case ANGLER -> new PotionEffect(PotionEffectType.WATER_BREATHING, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case SKYBOUND -> new PotionEffect(PotionEffectType.SLOW_FALLING, TITLE_EFFECT_DURATION_TICKS, 0, true, false, false);
            case WANDERER -> null;
        };
        if (effect != null) {
            player.addPotionEffect(effect);
        }
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
            if (instance.getModifier(modifier.uuid()) == null) {
                instance.addModifier(modifier.asModifier());
            }
        }
    }

    private static void clearTitleAttributeModifiers(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) {
                continue;
            }
            for (UUID id : TITLE_MODIFIER_IDS) {
                AttributeModifier existing = instance.getModifier(id);
                if (existing != null) {
                    instance.removeModifier(existing);
                }
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
