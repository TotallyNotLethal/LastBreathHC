package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModel.PlayerRowFields;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;

public final class BukkitTabMenuUpdateHandler implements TabMenuUpdateHandler {
    private final Map<String, Component> lastPlayerNames = new HashMap<>();

    @Override
    public void apply(TabMenuUpdate update) {
        if (update == null || update.isEmpty()) {
            return;
        }
        if (update.sections().contains(TabMenuSection.HEADER) || update.sections().contains(TabMenuSection.FOOTER)) {
            applyHeaderFooter(update);
        }
        if (update.sections().contains(TabMenuSection.PLAYERS)) {
            applyPlayers(update);
        }
    }

    private void applyHeaderFooter(TabMenuUpdate update) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendPlayerListHeaderAndFooter(update.renderResult().header(), update.renderResult().footer());
        }
    }

    private void applyPlayers(TabMenuUpdate update) {
        Map<String, Component> nextNames = new HashMap<>();
        List<PlayerRowFields> players = update.model().players();
        List<Component> playerLines = update.renderResult().playerLines();
        int count = Math.min(players.size(), playerLines.size());
        for (int i = 0; i < count; i++) {
            PlayerRowFields row = players.get(i);
            nextNames.put(row.username(), playerLines.get(i));
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            String username = player.getName();
            Component desired = nextNames.get(username);
            if (desired == null) {
                continue;
            }
            Component previous = lastPlayerNames.get(username);
            if (!Objects.equals(previous, desired)) {
                player.playerListName(desired);
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
        String displayName = row.displayName();
        builder.append(displayName != null && !displayName.isBlank() ? displayName : row.username());
        if (row.suffix() != null && !row.suffix().isBlank()) {
            builder.append(row.suffix());
        }
        return builder.toString().trim();
    }
}
