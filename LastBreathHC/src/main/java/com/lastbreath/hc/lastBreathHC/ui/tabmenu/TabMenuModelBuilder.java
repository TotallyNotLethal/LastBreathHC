package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.TabMenuConfig.RankStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class TabMenuModelBuilder {
    private final TabMenuConfig config;

    public TabMenuModelBuilder(TabMenuConfig config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    public static TabMenuModelBuilder load(LastBreathHC plugin) {
        return new TabMenuModelBuilder(TabMenuConfig.load(plugin));
    }

    public TabMenuModel build(TabMenuContext context, List<PlayerEntry> players) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(players, "players");

        Map<String, String> tokens = new HashMap<>();
        tokens.put("serverName", context.serverName());
        tokens.put("onlineCount", Integer.toString(context.onlineCount()));
        tokens.put("onlineCountFormatted", formatNumber(context.onlineCount()));
        tokens.put("pingMillis", Integer.toString(context.pingMillis()));
        tokens.put("pingMillisFormatted", formatNumber(context.pingMillis()));
        tokens.put("uniqueJoins", Integer.toString(context.uniqueJoins()));
        tokens.put("uniqueJoinsFormatted", formatNumber(context.uniqueJoins()));
        tokens.put("totalDeaths", Integer.toString(context.totalDeaths()));
        tokens.put("totalDeathsFormatted", formatNumber(context.totalDeaths()));
        tokens.put("totalPlaytime", emptyIfNull(context.totalPlaytime()));
        tokens.put("dateTimeLine", emptyIfNull(context.dateTimeLine()));
        tokens.put("playerListLine", emptyIfNull(context.playerListLine()));
        tokens.put("playerCountLine", emptyIfNull(context.playerCountLine()));

        String onlineSection = config.sections().showOnline()
                ? resolveTokens(config.segments().online(), tokens)
                : "";
        String pingSection = config.sections().showPing()
                ? resolveTokens(config.segments().ping(), tokens)
                : "";
        String joinsSection = config.sections().showJoins()
                ? resolveTokens(config.segments().joins(), tokens)
                : "";
        String deathsSection = config.sections().showDeaths()
                ? resolveTokens(config.segments().deaths(), tokens)
                : "";

        tokens.put("onlineSection", onlineSection);
        tokens.put("pingSection", pingSection);
        tokens.put("joinsSection", joinsSection);
        tokens.put("deathsSection", deathsSection);

        List<TabMenuModel.LineFields> headerLines = new ArrayList<>();
        for (TabMenuConfig.TemplateLine line : config.header().lines()) {
            String resolved = normalizeSpacing(resolveTokens(line.text(), tokens));
            if (!resolved.isBlank()) {
                headerLines.add(new TabMenuModel.LineFields(resolved, line.color()));
            }
        }

        List<TabMenuModel.PlayerRowFields> playerRows = new ArrayList<>(players.size());
        for (PlayerEntry player : players) {
            RankStyle rankStyle = rankStyleFor(player.rank());
            String icon = rankStyle != null ? rankStyle.icon() : null;
            String color = rankStyle != null ? rankStyle.color() : null;
            playerRows.add(new TabMenuModel.PlayerRowFields(
                    player.username(),
                    player.displayName(),
                    icon,
                    player.prefix(),
                    player.suffix(),
                    player.pingBars(),
                    player.pingMillis(),
                    color
            ));
        }

        List<TabMenuModel.LineFields> footerLines = new ArrayList<>();
        for (TabMenuConfig.TemplateLine line : config.footer().lines()) {
            String resolved = normalizeSpacing(resolveTokens(line.text(), tokens));
            if (!resolved.isBlank()) {
                footerLines.add(new TabMenuModel.LineFields(resolved, line.color()));
            }
        }

        TabMenuModel.HeaderFields header = new TabMenuModel.HeaderFields(headerLines);
        TabMenuModel.FooterFields footer = new TabMenuModel.FooterFields(footerLines, config.footer().urls());
        return new TabMenuModel(header, playerRows, footer);
    }

    private RankStyle rankStyleFor(String rank) {
        if (rank == null || rank.isBlank()) {
            return null;
        }
        return config.rankStyles().get(rank.toLowerCase(Locale.ROOT));
    }

    private String resolveTokens(String template, Map<String, String> tokens) {
        String resolved = template == null ? "" : template;
        for (Map.Entry<String, String> entry : tokens.entrySet()) {
            resolved = resolved.replace("{" + entry.getKey() + "}", emptyIfNull(entry.getValue()));
        }
        return resolved;
    }

    private String normalizeSpacing(String input) {
        if (input == null) {
            return "";
        }
        return input.replaceAll("\\s{2,}", " ").trim();
    }

    private String formatNumber(int value) {
        return String.format("%,d", value);
    }

    private String emptyIfNull(String value) {
        return value == null ? "" : value;
    }

    public record TabMenuContext(String serverName,
                                 int onlineCount,
                                 int pingMillis,
                                 int uniqueJoins,
                                 int totalDeaths,
                                 String totalPlaytime,
                                 String dateTimeLine,
                                 String playerListLine,
                                 String playerCountLine) {
    }

    public record PlayerEntry(String username,
                              String displayName,
                              String rank,
                              String prefix,
                              String suffix,
                              int pingBars,
                              int pingMillis) {
    }
}
