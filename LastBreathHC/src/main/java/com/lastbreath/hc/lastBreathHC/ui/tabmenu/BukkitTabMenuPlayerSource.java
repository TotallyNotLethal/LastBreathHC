package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticManager;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModelBuilder.PlayerEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitTabMenuPlayerSource implements TabMenuPlayerSource {

    @Override
    public List<PlayerEntry> getPlayers() {
        List<PlayerEntry> entries = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            String prefix = TitleManager.getTitleTabTag(player) + CosmeticManager.getPrefixTag(player, true);
            PlayerStats stats = StatsManager.get(player.getUniqueId());
            String nickname = stats != null ? stats.nickname : null;
            String displayName = nickname != null && !nickname.isBlank() ? nickname : player.getName();
            entries.add(new PlayerEntry(
                    player.getName(),
                    displayName,
                    null,
                    prefix != null && !prefix.isBlank() ? prefix : null,
                    null,
                    pingBarsFor(player.getPing()),
                    player.getPing()
            ));
        }
        entries.sort(Comparator.comparing(PlayerEntry::username, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private int pingBarsFor(int pingMillis) {
        if (pingMillis <= 75) {
            return 5;
        }
        if (pingMillis <= 150) {
            return 4;
        }
        if (pingMillis <= 250) {
            return 3;
        }
        if (pingMillis <= 350) {
            return 2;
        }
        return 1;
    }
}
