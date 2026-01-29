package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModel.PlayerRowFields;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitTabMenuUpdateHandler implements TabMenuUpdateHandler {
    private final Map<String, String> lastPlayerNames = new HashMap<>();

    @Override
    public void apply(TabMenuUpdate update) {
        if (update == null || update.isEmpty()) {
            return;
        }
        if (update.sections().contains(TabMenuSection.HEADER) || update.sections().contains(TabMenuSection.FOOTER)) {
            applyHeaderFooter(update);
        }
        if (update.sections().contains(TabMenuSection.PLAYERS)) {
            applyPlayers(update.model());
        }
    }

    private void applyHeaderFooter(TabMenuUpdate update) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(update.renderResult().header(), update.renderResult().footer());
        }
    }

    private void applyPlayers(TabMenuModel model) {
        Map<String, String> nextNames = new HashMap<>();
        for (PlayerRowFields row : model.players()) {
            String nextName = formatPlayerListName(row);
            nextNames.put(row.username(), nextName);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            String username = player.getName();
            String desired = nextNames.get(username);
            if (desired == null) {
                continue;
            }
            String previous = lastPlayerNames.get(username);
            if (!Objects.equals(previous, desired)) {
                player.setPlayerListName(desired);
            }
        }
        lastPlayerNames.clear();
        lastPlayerNames.putAll(nextNames);
    }

    private String formatPlayerListName(PlayerRowFields row) {
        StringBuilder builder = new StringBuilder();
        if (row.rankIcon() != null && !row.rankIcon().isBlank()) {
            builder.append(row.rankIcon()).append(' ');
        }
        if (row.prefix() != null && !row.prefix().isBlank()) {
            builder.append(row.prefix());
        }
        builder.append(row.username());
        if (row.suffix() != null && !row.suffix().isBlank()) {
            builder.append(row.suffix());
        }
        return builder.toString().trim();
    }
}
