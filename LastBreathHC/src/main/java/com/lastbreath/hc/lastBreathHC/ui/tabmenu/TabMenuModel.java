package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public record TabMenuModel(HeaderFields header,
                           List<PlayerRowFields> players,
                           FooterFields footer) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public TabMenuModel {
        Objects.requireNonNull(header, "header");
        Objects.requireNonNull(footer, "footer");
        players = List.copyOf(Objects.requireNonNull(players, "players"));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("header", header.toMap());
        payload.put("players", players.stream()
                .map(PlayerRowFields::toMap)
                .collect(Collectors.toList()));
        payload.put("footer", footer.toMap());
        return payload;
    }

    public record LineFields(String text,
                             String color) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("text", text);
            payload.put("color", color);
            return payload;
        }
    }

    public record HeaderFields(List<LineFields> lines) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public HeaderFields {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("lines", lines.stream()
                    .map(LineFields::toMap)
                    .collect(Collectors.toList()));
            return payload;
        }
    }

    public record PlayerRowFields(String username,
                                  String displayName,
                                  String rankIcon,
                                  String prefix,
                                  String suffix,
                                  int pingBars,
                                  String customColor) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", username);
            payload.put("displayName", displayName);
            payload.put("rankIcon", rankIcon);
            payload.put("prefix", prefix);
            payload.put("suffix", suffix);
            payload.put("pingBars", pingBars);
            payload.put("customColor", customColor);
            return payload;
        }
    }

    public record FooterFields(List<LineFields> lines,
                               List<String> urls) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public FooterFields {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            urls = List.copyOf(Objects.requireNonNull(urls, "urls"));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("lines", lines.stream()
                    .map(LineFields::toMap)
                    .collect(Collectors.toList()));
            payload.put("urls", urls);
            return payload;
        }
    }
}
