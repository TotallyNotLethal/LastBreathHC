package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager.StatsSummary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabMenuDataSource {
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(10);

    private final LastBreathHC plugin;
    private final Duration refreshInterval;
    private final Clock clock;
    private TabMenuModelBuilder.TabMenuContext cachedContext;
    private Instant lastRefresh = Instant.EPOCH;

    public TabMenuDataSource(LastBreathHC plugin) {
        this(plugin, DEFAULT_REFRESH_INTERVAL, Clock.systemUTC());
    }

    public TabMenuDataSource(LastBreathHC plugin, Duration refreshInterval) {
        this(plugin, refreshInterval, Clock.systemUTC());
    }

    public TabMenuDataSource(LastBreathHC plugin, Duration refreshInterval, Clock clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.refreshInterval = Objects.requireNonNull(refreshInterval, "refreshInterval");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public TabMenuModelBuilder.TabMenuContext getContext() {
        if (refreshInterval.isZero() || refreshInterval.isNegative()) {
            return buildSnapshot();
        }
        if (cachedContext == null || Duration.between(lastRefresh, Instant.now(clock)).compareTo(refreshInterval) >= 0) {
            refresh();
        }
        return cachedContext;
    }

    public void refresh() {
        cachedContext = buildSnapshot();
        lastRefresh = Instant.now(clock);
    }

    private TabMenuModelBuilder.TabMenuContext buildSnapshot() {
        String serverName = plugin.getConfig().getString("serverName");
        if (serverName == null || serverName.isBlank()) {
            serverName = Bukkit.getServer().getServerName();
        }
        StatsSummary summary = StatsManager.summarize();
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int pingMillis = calculateAveragePing(Bukkit.getOnlinePlayers());
        return new TabMenuModelBuilder.TabMenuContext(
                serverName,
                onlineCount,
                pingMillis,
                summary.uniqueJoins(),
                summary.totalDeaths(),
                null
        );
    }

    private int calculateAveragePing(Collection<? extends Player> players) {
        if (players.isEmpty()) {
            return 0;
        }
        double average = players.stream().mapToInt(Player::getPing).average().orElse(0.0);
        return (int) Math.round(average);
    }
}
