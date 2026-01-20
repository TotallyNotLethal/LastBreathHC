package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TitleManager {

    private static final Title DEFAULT_TITLE = Title.WANDERER;
    private static final long ONE_HOUR_SECONDS = 60 * 60;
    private static final long TEN_HOURS_SECONDS = 10 * 60 * 60;
    private static final long TWENTY_FIVE_HOURS_SECONDS = 25 * 60 * 60;
    private static final long ONE_HUNDRED_HOURS_SECONDS = 100 * 60 * 60;
    private static final Map<Title, List<String>> TITLE_EFFECTS = Map.of(
            Title.WANDERER, List.of()
    );

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
    }
}
