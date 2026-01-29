package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public record TabMenuConfig(Header header,
                            Footer footer,
                            Sections sections,
                            Segments segments,
                            Map<String, RankStyle> rankStyles) {

    private static final String RESOURCE_PATH = "tab-menu.yml";

    public TabMenuConfig {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(footer, "footer");
        Objects.requireNonNull(sections, "sections");
        Objects.requireNonNull(segments, "segments");
        rankStyles = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(rankStyles, "rankStyles")));
    }

    public static TabMenuConfig load(LastBreathHC plugin) {
        Objects.requireNonNull(plugin, "plugin");
        File dataFile = new File(plugin.getDataFolder(), RESOURCE_PATH);
        if (!dataFile.exists()) {
            plugin.saveResource(RESOURCE_PATH, false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        Header header = readHeader(config.getConfigurationSection("header"));
        Footer footer = readFooter(config.getConfigurationSection("footer"));
        Sections sections = readSections(config.getConfigurationSection("sections"));
        Segments segments = readSegments(config.getConfigurationSection("segments"));
        Map<String, RankStyle> rankStyles = readRankStyles(config.getConfigurationSection("ranks"));
        return new TabMenuConfig(header, footer, sections, segments, rankStyles);
    }

    private static Header readHeader(ConfigurationSection section) {
        List<TemplateLine> lines = readLines(section, "lines");
        if (lines.isEmpty()) {
            lines = List.of(
                    new TemplateLine("{serverName}", "gold"),
                    new TemplateLine("{onlineSection}{pingSection}{joinsSection}{deathsSection}", "gray")
            );
        }
        return new Header(lines);
    }

    private static Footer readFooter(ConfigurationSection section) {
        List<TemplateLine> lines = readLines(section, "lines");
        List<String> urls = section != null ? section.getStringList("urls") : List.of();
        return new Footer(lines, urls);
    }

    private static Sections readSections(ConfigurationSection section) {
        if (section == null) {
            return new Sections(true, true, true, true);
        }
        return new Sections(
                section.getBoolean("showOnline", true),
                section.getBoolean("showPing", true),
                section.getBoolean("showJoins", true),
                section.getBoolean("showDeaths", true)
        );
    }

    private static Segments readSegments(ConfigurationSection section) {
        if (section == null) {
            return Segments.defaultSegments();
        }
        String online = section.getString("online", Segments.defaultSegments().online());
        String ping = section.getString("ping", Segments.defaultSegments().ping());
        String joins = section.getString("joins", Segments.defaultSegments().joins());
        String deaths = section.getString("deaths", Segments.defaultSegments().deaths());
        return new Segments(online, ping, joins, deaths);
    }

    private static Map<String, RankStyle> readRankStyles(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }
        Map<String, RankStyle> result = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(key);
            if (entry == null) {
                continue;
            }
            String icon = entry.getString("icon");
            String color = entry.getString("color");
            result.put(key.toLowerCase(Locale.ROOT), new RankStyle(icon, color));
        }
        return result;
    }

    private static List<TemplateLine> readLines(ConfigurationSection section, String path) {
        if (section == null) {
            return List.of();
        }
        List<Map<?, ?>> maps = section.getMapList(path);
        List<TemplateLine> lines = new ArrayList<>();
        for (Map<?, ?> entry : maps) {
            Object text = entry.get("text");
            if (text == null) {
                continue;
            }
            Object color = entry.get("color");
            lines.add(new TemplateLine(text.toString(), color != null ? color.toString() : null));
        }
        return lines;
    }

    public record Header(List<TemplateLine> lines) {
        public Header {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }
    }

    public record Footer(List<TemplateLine> lines,
                         List<String> urls) {
        public Footer {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            urls = List.copyOf(Objects.requireNonNull(urls, "urls"));
        }
    }

    public record Sections(boolean showOnline,
                           boolean showPing,
                           boolean showJoins,
                           boolean showDeaths) {
    }

    public record Segments(String online,
                           String ping,
                           String joins,
                           String deaths) {
        public static Segments defaultSegments() {
            return new Segments(
                    "Online: {onlineCount}  ",
                    "Ping: {pingMillis}ms  ",
                    "Unique Joins: {uniqueJoins}  ",
                    "Total Deaths: {totalDeaths}"
            );
        }
    }

    public record RankStyle(String icon, String color) {
    }

    public record TemplateLine(String text, String color) {
    }
}
