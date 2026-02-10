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
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BukkitTabMenuPlayerSource implements TabMenuPlayerSource {
    private static final int MIN_FAKE_TAB_PING_MILLIS = 45;
    private static final int MAX_FAKE_TAB_PING_MILLIS = 160;
    private static final int FAKE_TAB_PING_VARIANCE_MILLIS = 5;

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
                Title fakeTitle = resolveTitle(record.getTabTitleKey());
                int fakeBasePing = normalizedFakeBasePing(record.getTabPingMillis());
                int fakePing = jitteredFakePing(fakeBasePing);
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

    private int normalizedFakeBasePing(int configuredPingMillis) {
        return Math.max(MIN_FAKE_TAB_PING_MILLIS, Math.min(MAX_FAKE_TAB_PING_MILLIS, configuredPingMillis));
    }

    private int jitteredFakePing(int basePingMillis) {
        int jitter = ThreadLocalRandom.current().nextInt(-FAKE_TAB_PING_VARIANCE_MILLIS, FAKE_TAB_PING_VARIANCE_MILLIS + 1);
        int jitteredPing = basePingMillis + jitter;
        return Math.max(MIN_FAKE_TAB_PING_MILLIS, Math.min(MAX_FAKE_TAB_PING_MILLIS, jitteredPing));
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

    private Title resolveTitle(String tabTitleKey) {
        Title title = Title.fromInput(tabTitleKey);
        return title == null ? Title.WANDERER : title;
    }
}
