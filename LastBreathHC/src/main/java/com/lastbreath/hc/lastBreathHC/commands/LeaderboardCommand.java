package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.LeaderboardGUI;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class LeaderboardCommand implements BasicCommand {

    private static final int PAGE_SIZE = 10;

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (args.length == 0) {
            if (sender instanceof Player player) {
                LeaderboardGUI.openMetricSelect(player);
            } else {
                sender.sendMessage(ChatColor.RED + "Usage: /leaderboard <metric> [page]");
            }
            return;
        }

        if (args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /leaderboard <metric> [page]");
            return;
        }

        StatsManager.LeaderboardMetric metric = StatsManager.LeaderboardMetric.fromInput(args[0]);
        if (metric == null) {
            sender.sendMessage(ChatColor.RED + "Unknown metric. Available: " + String.join(", ",
                    Arrays.stream(StatsManager.LeaderboardMetric.values()).map(StatsManager.LeaderboardMetric::key).toList()));
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) {
                    sender.sendMessage(ChatColor.RED + "Page must be 1 or greater.");
                    return;
                }
            } catch (NumberFormatException ignored) {
                sender.sendMessage(ChatColor.RED + "Invalid page. Usage: /leaderboard <metric> [page]");
                return;
            }
        }

        if (sender instanceof Player player && args.length == 1) {
            LeaderboardGUI.openEntries(player, metric, page);
            return;
        }

        sendChatLeaderboard(sender, metric, page);
    }

    private void sendChatLeaderboard(CommandSender sender, StatsManager.LeaderboardMetric metric, int page) {
        int fetchSize = page * PAGE_SIZE;
        List<StatsManager.LeaderboardEntry> entries = StatsManager.getLeaderboard(metric, fetchSize);
        if (entries.isEmpty()) {
            sender.sendMessage(ChatColor.YELLOW + "No leaderboard entries found for " + metric.displayName() + ".");
            return;
        }

        int totalPages = Math.max(1, (int) Math.ceil(entries.size() / (double) PAGE_SIZE));
        if (page > totalPages) {
            page = totalPages;
        }

        int start = (page - 1) * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, entries.size());

        sender.sendMessage(ChatColor.GOLD + "Leaderboard: " + metric.displayName() + ChatColor.GRAY + " (Page " + page + "/" + totalPages + ")");
        for (int i = start; i < end; i++) {
            StatsManager.LeaderboardEntry entry = entries.get(i);
            String value = metric == StatsManager.LeaderboardMetric.PLAYTIME
                    ? StatsManager.formatTicks(entry.value())
                    : String.valueOf(entry.value());
            String displayName = entry.banned()
                    ? ChatColor.STRIKETHROUGH + entry.displayName() + ChatColor.RESET
                    : entry.displayName();
            sender.sendMessage(ChatColor.YELLOW + "#" + (i + 1) + ChatColor.GRAY + " " + displayName + ChatColor.DARK_GRAY + " - " + ChatColor.WHITE + value);
        }
    }
}
