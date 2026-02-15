package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.cosmetics.CosmeticManager;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerService;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuModelBuilder.PlayerEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitTabMenuPlayerSource implements TabMenuPlayerSource {
    private final FakePlayerService fakePlayerService;

    public BukkitTabMenuPlayerSource(FakePlayerService fakePlayerService) {
        this.fakePlayerService = fakePlayerService;
    }

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
                    null
            ));
        }
        if (fakePlayerService != null) {
            for (FakePlayerRecord record : fakePlayerService.listFakePlayers()) {
                if (!record.isActive()) {
                    continue;
                }
                Title fakeTitle = resolveTitle(record.getTabTitleKey());
                String prefix = "§7[" + fakeTitle.tabTag() + "§7] ";
                entries.add(new PlayerEntry(
                        record.getName(),
                        record.getName(),
                        null,
                        prefix,
                        null
                ));
            }
        }
        entries.sort(Comparator.comparing(PlayerEntry::username, String.CASE_INSENSITIVE_ORDER));
        return entries;
    }

    private Title resolveTitle(String tabTitleKey) {
        Title title = Title.fromInput(tabTitleKey);
        return title == null ? Title.WANDERER : title;
    }
}
