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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitTabMenuPlayerSource implements TabMenuPlayerSource {
    private final FakePlayerService fakePlayerService;
    private final Map<UUID, Title> fakeTitles = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fakePings = new ConcurrentHashMap<>();

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
                    null,
                    pingBarsFor(player.getPing()),
                    player.getPing()
            ));
        }
        if (fakePlayerService != null) {
            for (FakePlayerRecord record : fakePlayerService.listFakePlayers()) {
                if (!record.isActive()) {
                    continue;
                }
                Title fakeTitle = fakeTitles.computeIfAbsent(record.getUuid(), key -> randomTitle());
                int fakePing = fakePings.computeIfAbsent(record.getUuid(), key -> randomPingMillis());
                String prefix = "§7[" + fakeTitle.tabTag() + "§7] ";
                entries.add(new PlayerEntry(
                        record.getName(),
                        record.getName(),
                        null,
                        prefix,
                        null,
                        pingBarsFor(fakePing),
                        fakePing
                ));
            }
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

    private Title randomTitle() {
        Title[] titles = Title.values();
        return titles[ThreadLocalRandom.current().nextInt(titles.length)];
    }

    private int randomPingMillis() {
        return ThreadLocalRandom.current().nextInt(40, 141);
    }
}
