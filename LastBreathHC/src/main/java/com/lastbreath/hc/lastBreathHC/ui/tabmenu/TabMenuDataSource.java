package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager;
import com.lastbreath.hc.lastBreathHC.stats.StatsManager.StatsSummary;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuConfig.DateTimeSettings;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collection;
import java.util.Objects;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class TabMenuDataSource {
    private static final Duration DEFAULT_REFRESH_INTERVAL = Duration.ofSeconds(10);

    private final LastBreathHC plugin;
    private final Duration refreshInterval;
    private final Clock clock;
    private final DateTimeSettings dateTimeSettings;
    private TabMenuModelBuilder.TabMenuContext cachedContext;
    private Instant lastRefresh = Instant.EPOCH;

    public TabMenuDataSource(LastBreathHC plugin, DateTimeSettings dateTimeSettings) {
        this(plugin, dateTimeSettings, DEFAULT_REFRESH_INTERVAL, Clock.systemUTC());
    }

    public TabMenuDataSource(LastBreathHC plugin, DateTimeSettings dateTimeSettings, Duration refreshInterval) {
        this(plugin, dateTimeSettings, refreshInterval, Clock.systemUTC());
    }

    public TabMenuDataSource(LastBreathHC plugin, DateTimeSettings dateTimeSettings, Duration refreshInterval, Clock clock) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.dateTimeSettings = Objects.requireNonNull(dateTimeSettings, "dateTimeSettings");
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
        String serverName = "Last Breath";
        StatsSummary summary = StatsManager.summarize();
        int onlineCount = Bukkit.getOnlinePlayers().size();
        int pingMillis = calculateAveragePing(Bukkit.getOnlinePlayers());
        String dateTimeLine = buildDateTimeLine();
        String playerCountLine = "Online players: " + onlineCount + " | Ping: " + pingMillis + "ms";
        return new TabMenuModelBuilder.TabMenuContext(
                serverName,
                onlineCount,
                pingMillis,
                summary.uniqueJoins(),
                Bukkit.getBannedPlayers().size(),
                dateTimeLine,
                null,
                playerCountLine
        );
    }

    private int calculateAveragePing(Collection<? extends Player> players) {
        if (players.isEmpty()) {
            return 0;
        }
        double average = players.stream().mapToInt(Player::getPing).average().orElse(0.0);
        return (int) Math.round(average);
    }

    private String buildDateTimeLine() {
        if (!dateTimeSettings.enabled()) {
            return null;
        }
        DateTimeFormatter formatter = safeFormatter(dateTimeSettings.format());
        ZoneId zoneId = safeZone(dateTimeSettings.zoneId());
        return formatter.format(ZonedDateTime.now(clock).withZoneSameInstant(zoneId));
    }

    private DateTimeFormatter safeFormatter(String format) {
        String pattern = format == null || format.isBlank()
                ? DateTimeSettings.defaultSettings().format()
                : format;
        try {
            return DateTimeFormatter.ofPattern(pattern);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return DateTimeFormatter.ofPattern(DateTimeSettings.defaultSettings().format());
        }
    }

    private ZoneId safeZone(String zoneId) {
        String zone = zoneId == null || zoneId.isBlank()
                ? DateTimeSettings.defaultSettings().zoneId()
                : zoneId;
        try {
            return ZoneId.of(zone);
        } catch (Exception e) {
            return ZoneId.of(DateTimeSettings.defaultSettings().zoneId());
        }
    }
}
