package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerService;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModel.PlayerRowFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class BukkitTabMenuUpdateHandler implements TabMenuUpdateHandler {
    private final Map<String, String> lastPlayerNames = new HashMap<>();
    private final Set<UUID> lastAudience = new HashSet<>();
    private final FakePlayerService fakePlayerService;

    public BukkitTabMenuUpdateHandler() {
        this(null);
    }

    public BukkitTabMenuUpdateHandler(FakePlayerService fakePlayerService) {
        this.fakePlayerService = fakePlayerService;
    }

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
        Set<UUID> nextAudience = new HashSet<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            nextAudience.add(player.getUniqueId());
        }
        boolean audienceChanged = !nextAudience.equals(lastAudience);

        Map<String, String> nextNames = new HashMap<>();
        Map<String, String> nextNamesByLower = new HashMap<>();
        for (PlayerRowFields row : model.players()) {
            String nextName = formatPlayerListName(row);
            nextNames.put(row.username(), nextName);
            nextNamesByLower.put(row.username().toLowerCase(Locale.ROOT), nextName);
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
        if (fakePlayerService != null) {
            for (FakePlayerRecord record : fakePlayerService.listFakePlayers()) {
                if (!record.isActive()) {
                    continue;
                }
                String username = record.getName();
                String desired = nextNames.get(username);
                if (desired == null) {
                    desired = nextNamesByLower.get(username.toLowerCase(Locale.ROOT));
                }
                if (desired == null) {
                    continue;
                }
                String previous = lastPlayerNames.get(username);
                boolean changed = !Objects.equals(previous, desired);
                Player fakePlayer = fakePlayerService.resolveBukkitPlayer(record).orElse(null);
                if (fakePlayer == null) {
                    if (changed || audienceChanged) {
                        fakePlayerService.refreshVisual(record.getUuid());
                    }
                    continue;
                }
                if (changed) {
                    fakePlayer.setPlayerListName(desired);
                }
                if (changed || audienceChanged) {
                    fakePlayerService.refreshVisual(record.getUuid());
                }
            }
        }
        lastPlayerNames.clear();
        lastPlayerNames.putAll(nextNames);
        lastAudience.clear();
        lastAudience.addAll(nextAudience);
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
        builder.append(' ')
                .append(ChatColor.GRAY)
                .append(row.pingMillis())
                .append("ms");
        return builder.toString().trim();
    }
}
