package com.lastbreath.hc.lastBreathHC.titles;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.stream.Collectors;

public class TitleManager {

    private static final Title DEFAULT_TITLE = Title.WANDERER;
    private static final long ONE_HOUR_SECONDS = 60 * 60;
    private static final long TWO_HOURS_SECONDS = 2 * 60 * 60;
    private static final long FOUR_HOURS_SECONDS = 4 * 60 * 60;
    private static final long SIX_HOURS_SECONDS = 6 * 60 * 60;

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
        if (seconds >= TWO_HOURS_SECONDS) {
            unlockTitle(player, Title.IRON_WILL, "Survived for at least two hours.");
        }
        if (seconds >= FOUR_HOURS_SECONDS) {
            unlockTitle(player, Title.VOID_WALKER, "Survived for at least four hours.");
        }
        if (seconds >= SIX_HOURS_SECONDS) {
            unlockTitle(player, Title.LAST_SURVIVOR, "Survived for at least six hours.");
        }
    }
}
