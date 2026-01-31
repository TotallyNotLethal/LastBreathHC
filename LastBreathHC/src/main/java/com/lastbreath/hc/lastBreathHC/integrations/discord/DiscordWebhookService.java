package com.lastbreath.hc.lastBreathHC.integrations.discord;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public class DiscordWebhookService {

    private static final String CONFIG_ROOT = "discordWebhook";
    private static final int DEFAULT_EMBED_COLOR = 0x7b1f1f;
    private final LastBreathHC plugin;

    public DiscordWebhookService(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    public void sendDeathWebhook(Player player,
                                 PlayerStats stats,
                                 String deathReason,
                                 String killerLabel,
                                 Location location,
                                 boolean hasReviveToken) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".enabled", false)) {
            return;
        }
        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".url", "").trim();
        if (webhookUrl.isEmpty()) {
            return;
        }

        String username = plugin.getConfig().getString(CONFIG_ROOT + ".username", "Charon");
        String avatarUrl = plugin.getConfig().getString(CONFIG_ROOT + ".avatarUrl", "").trim();
        int embedColor = parseEmbedColor(
                plugin.getConfig().getString(CONFIG_ROOT + ".embedColor", "")
        );
        String footerText = plugin.getConfig().getString(CONFIG_ROOT + ".footerText", "Charon, Ferryman of the Fallen");

        String timePlayed = formatDuration(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);
        String locationLabel = formatLocation(location);
        String reviveLabel = hasReviveToken ? "Token present" : "No token";

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        if (!avatarUrl.isEmpty()) {
            payload.put("avatar_url", avatarUrl);
        }

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "☠ Soul Ferried");
        embed.put("description", "**" + player.getName() + "** has fallen.");
        embed.put("color", embedColor);
        embed.put("timestamp", Instant.now().toString());
        embed.put("footer", Map.of("text", footerText));
        embed.put("fields", new Object[]{
                Map.of("name", "Death Cause", "value", safeValue(deathReason), "inline", false),
                Map.of("name", "Time Played", "value", timePlayed, "inline", true),
                Map.of("name", "Deaths", "value", String.valueOf(stats.deaths), "inline", true),
                Map.of("name", "Revives", "value", String.valueOf(stats.revives), "inline", true),
                Map.of("name", "Mobs Slain", "value", String.valueOf(stats.mobsKilled), "inline", true),
                Map.of("name", "Blocks Mined", "value", String.valueOf(stats.blocksMined), "inline", true),
                Map.of("name", "Crops Harvested", "value", String.valueOf(stats.cropsHarvested), "inline", true),
                Map.of("name", "Rare Ores", "value", String.valueOf(stats.rareOresMined), "inline", true),
                Map.of("name", "Killer", "value", safeValue(killerLabel), "inline", true),
                Map.of("name", "Revive Token", "value", reviveLabel, "inline", true),
                Map.of("name", "Location", "value", locationLabel, "inline", false)
        });
        payload.put("embeds", new Object[]{embed});

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postPayload(webhookUrl, payload));
    }

    private void postPayload(String webhookUrl, Map<String, Object> payload) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(webhookUrl).toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            byte[] body = toJson(payload).getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            try (OutputStream outputStream = connection.getOutputStream()) {
                outputStream.write(body);
            }

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("Discord webhook returned HTTP " + code + " for death payload.");
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to deliver Discord webhook: " + e.getMessage());
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String formatLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Unknown";
        }
        return location.getWorld().getName()
                + " (" + location.getBlockX()
                + ", " + location.getBlockY()
                + ", " + location.getBlockZ() + ")";
    }

    private String formatDuration(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;
        StringBuilder builder = new StringBuilder();
        if (hours > 0) {
            builder.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0) {
            builder.append(minutes).append("m ");
        }
        builder.append(remainingSeconds).append("s");
        return builder.toString().trim();
    }

    private int parseEmbedColor(String colorValue) {
        if (colorValue == null || colorValue.isBlank()) {
            return DEFAULT_EMBED_COLOR;
        }
        String sanitized = colorValue.trim();
        if (sanitized.startsWith("#")) {
            sanitized = sanitized.substring(1);
        }
        if (sanitized.startsWith("0x") || sanitized.startsWith("0X")) {
            sanitized = sanitized.substring(2);
        }
        try {
            return Integer.parseInt(sanitized, 16);
        } catch (NumberFormatException e) {
            return DEFAULT_EMBED_COLOR;
        }
    }

    private String safeValue(String value) {
        return value == null || value.isBlank() ? "Unknown" : value;
    }

    private String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof String stringValue) {
            return "\"" + escapeJson(stringValue) + "\"";
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof Map<?, ?> mapValue) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : mapValue.entrySet()) {
                if (!(entry.getKey() instanceof String key)) {
                    continue;
                }
                if (!first) {
                    builder.append(',');
                }
                builder.append("\"").append(escapeJson(key)).append("\":");
                builder.append(toJson(entry.getValue()));
                first = false;
            }
            return builder.append('}').toString();
        }
        if (value instanceof Object[] arrayValue) {
            StringBuilder builder = new StringBuilder("[");
            for (int i = 0; i < arrayValue.length; i++) {
                if (i > 0) {
                    builder.append(',');
                }
                builder.append(toJson(arrayValue[i]));
            }
            return builder.append(']').toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (c < 0x20) {
                        builder.append(String.format("\\u%04x", (int) c));
                    } else {
                        builder.append(c);
                    }
                }
            }
        }
        return builder.toString();
    }
}
