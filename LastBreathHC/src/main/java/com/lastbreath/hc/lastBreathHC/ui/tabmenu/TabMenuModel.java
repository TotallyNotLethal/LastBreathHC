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

    public record HeaderFields(String serverName,
                               int onlineCount,
                               int pingMillis,
                               int uniqueJoins,
                               int totalDeaths) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("serverName", serverName);
            payload.put("onlineCount", onlineCount);
            payload.put("pingMillis", pingMillis);
            payload.put("uniqueJoins", uniqueJoins);
            payload.put("totalDeaths", totalDeaths);
            return payload;
        }
    }

    public record PlayerRowFields(String username,
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
            payload.put("rankIcon", rankIcon);
            payload.put("prefix", prefix);
            payload.put("suffix", suffix);
            payload.put("pingBars", pingBars);
            payload.put("customColor", customColor);
            return payload;
        }
    }

    public record FooterFields(String dateTimeLine,
                               List<String> announcements,
                               List<String> urls) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        public FooterFields {
            announcements = List.copyOf(Objects.requireNonNull(announcements, "announcements"));
            urls = List.copyOf(Objects.requireNonNull(urls, "urls"));
        }

        public Map<String, Object> toMap() {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("dateTimeLine", dateTimeLine);
            payload.put("announcements", announcements);
            payload.put("urls", urls);
            return payload;
        }
    }
}
