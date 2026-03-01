package com.lastbreath.hc.lastBreathHC.integrations.discord;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.stats.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordWebhookService {

    private static final String CONFIG_ROOT = "discordWebhook";
    private static final int DEFAULT_EMBED_COLOR = 0x7b1f1f;
    private static final int WEBHOOK_PAGE_SIZE = 100;
    private static final int WEBHOOK_DELETE_PAGE_CAP = 25;
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("\\\"id\\\"\\s*:\\s*\\\"(\\d+)\\\"");
    private final LastBreathHC plugin;

    public DiscordWebhookService(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    public record AsteroidSpawnInfo(Location location, int tier) {
    }

    public void sendDeathWebhook(Player player,
                                 PlayerStats stats,
                                 String deathReason,
                                 String killerLabel,
                                 Location location,
                                 boolean hasReviveToken,
                                 String lastMessageBeforeDeath) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".enabled", false)) {
            plugin.getLogger().info("Discord webhook disabled; skipping death notification. player="
                    + player.getName());
            return;
        }
        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".url", "").trim();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().info("Discord webhook URL empty; skipping death notification. player="
                    + player.getName());
            return;
        }

        boolean isPermanentDeath = !hasReviveToken;
        plugin.getLogger().info("Discord webhook send started. player=" + player.getName()
                + " deathReason=" + safeValue(deathReason)
                + " permanentDeath=" + isPermanentDeath);

        String username = plugin.getConfig().getString(CONFIG_ROOT + ".username", "Charon");
        String avatarUrl = plugin.getConfig().getString(CONFIG_ROOT + ".avatarUrl", "").trim();
        int embedColor = parseEmbedColor(
                plugin.getConfig().getString(CONFIG_ROOT + ".embedColor", "")
        );
        String footerText = plugin.getConfig().getString(CONFIG_ROOT + ".footerText", "Charon, Ferryman of the Fallen");

        String timePlayed = formatDuration(player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20);
        String locationLabel = formatLocation(location);
        String reviveLabel = hasReviveToken ? "Token present" : "No token";
        String lastMessage = safeValue(lastMessageBeforeDeath);

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
        embed.put("thumbnail", Map.of("url", "https://render.crafty.gg/3d/bust/" + player.getName()));
        embed.put("footer", Map.of("text", footerText));
        embed.put("fields", new Object[]{
                Map.of("name", "Death Cause", "value", safeValue(deathReason), "inline", false),
                Map.of("name", "Time Played", "value", timePlayed, "inline", true),
                //Map.of("name", "Deaths", "value", String.valueOf(stats.deaths), "inline", true),
                //Map.of("name", "Revives", "value", String.valueOf(stats.revives), "inline", true),
                Map.of("name", "Mobs Slain", "value", String.valueOf(stats.mobsKilled), "inline", true),
                Map.of("name", "Blocks Mined", "value", String.valueOf(stats.blocksMined), "inline", true),
                Map.of("name", "Last Message", "value", lastMessage, "inline", false),
                //Map.of("name", "Crops Harvested", "value", String.valueOf(stats.cropsHarvested), "inline", true),
                //Map.of("name", "Rare Ores", "value", String.valueOf(stats.rareOresMined), "inline", true),
                //Map.of("name", "Killer", "value", safeValue(killerLabel), "inline", true),
                //Map.of("name", "Revive Token", "value", reviveLabel, "inline", true),
                //Map.of("name", "Location", "value", locationLabel, "inline", false)
        });
        payload.put("embeds", new Object[]{embed});

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postPayload(webhookUrl, payload));
    }

    public void sendAsteroidCrashWebhook(Location loc, int tier, boolean meteorShowerContext) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".asteroidsEnabled", false)) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUrl", "").trim();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().info("Asteroid Discord webhook URL empty; skipping asteroid crash notification.");
            return;
        }

        String username = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUsername",
                plugin.getConfig().getString(CONFIG_ROOT + ".username", "Charon"));
        String avatarUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsAvatarUrl",
                plugin.getConfig().getString(CONFIG_ROOT + ".avatarUrl", "")).trim();
        int embedColor = parseEmbedColor(plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsEmbedColor",
                plugin.getConfig().getString(CONFIG_ROOT + ".embedColor", "")));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        if (!avatarUrl.isEmpty()) {
            payload.put("avatar_url", avatarUrl);
        }

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", meteorShowerContext ? "☄ Meteor Shower Impact" : "☄ Asteroid Impact");
        embed.put("description", "An asteroid has crashed.");
        embed.put("color", embedColor);
        embed.put("timestamp", Instant.now().toString());
        embed.put("fields", new Object[]{
                Map.of("name", "World", "value", formatWorld(loc), "inline", true),
                Map.of("name", "Coordinates", "value", formatCoordinates(loc), "inline", true),
                Map.of("name", "Tier", "value", String.valueOf(tier), "inline", true)
        });

        payload.put("embeds", new Object[]{embed});
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postPayload(webhookUrl, payload));
    }

    public void sendMeteorShowerWebhook(List<AsteroidSpawnInfo> asteroids) {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".asteroidsEnabled", false) || asteroids == null || asteroids.isEmpty()) {
            return;
        }

        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUrl", "").trim();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().info("Asteroid Discord webhook URL empty; skipping meteor shower notification.");
            return;
        }

        String username = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUsername",
                plugin.getConfig().getString(CONFIG_ROOT + ".username", "Charon"));
        String avatarUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsAvatarUrl",
                plugin.getConfig().getString(CONFIG_ROOT + ".avatarUrl", "")).trim();
        int embedColor = parseEmbedColor(plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsEmbedColor",
                plugin.getConfig().getString(CONFIG_ROOT + ".embedColor", "")));

        StringBuilder listBuilder = new StringBuilder();
        int index = 1;
        for (AsteroidSpawnInfo asteroid : asteroids) {
            listBuilder.append(index++)
                    .append(". `")
                    .append(formatWorld(asteroid.location()))
                    .append(" | ")
                    .append(formatCoordinates(asteroid.location()))
                    .append(" | Tier ")
                    .append(asteroid.tier())
                    .append("`\n");
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        if (!avatarUrl.isEmpty()) {
            payload.put("avatar_url", avatarUrl);
        }

        Map<String, Object> embed = new LinkedHashMap<>();
        embed.put("title", "☄ Meteor Shower Detected");
        embed.put("description", "Multiple asteroids are inbound.");
        embed.put("color", embedColor);
        embed.put("timestamp", Instant.now().toString());
        embed.put("fields", new Object[]{
                Map.of("name", "Asteroids", "value", listBuilder.toString().trim(), "inline", false)
        });

        payload.put("embeds", new Object[]{embed});
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postPayload(webhookUrl, payload));
    }

    public void clearAsteroidWebhookMessages() {
        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUrl", "").trim();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().info("Asteroid Discord webhook URL empty; skipping startup asteroid message cleanup.");
            return;
        }

        String webhookMessagesEndpoint = resolveWebhookMessagesEndpoint(webhookUrl);
        if (webhookMessagesEndpoint == null) {
            plugin.getLogger().warning("Unable to parse asteroid webhook URL for startup cleanup. url=" + webhookUrl);
            return;
        }

        int deletedCount = 0;
        int failureCount = 0;
        String beforeMessageId = null;

        for (int page = 0; page < WEBHOOK_DELETE_PAGE_CAP; page++) {
            String listEndpoint = webhookMessagesEndpoint + "?limit=" + WEBHOOK_PAGE_SIZE;
            if (beforeMessageId != null) {
                listEndpoint = listEndpoint + "&before=" + beforeMessageId;
            }

            HttpResponse listResponse = executeWebhookRequest("GET", listEndpoint);
            if (listResponse == null) {
                failureCount++;
                break;
            }
            if (listResponse.statusCode() < 200 || listResponse.statusCode() >= 300) {
                failureCount++;
                plugin.getLogger().warning("Failed to fetch asteroid webhook messages during startup cleanup. code="
                        + listResponse.statusCode() + " body=" + safeValue(listResponse.body()));
                break;
            }

            List<String> messageIds = extractMessageIds(listResponse.body());
            if (messageIds.isEmpty()) {
                break;
            }

            beforeMessageId = messageIds.get(messageIds.size() - 1);
            for (String messageId : messageIds) {
                String deleteEndpoint = webhookMessagesEndpoint + "/" + messageId;
                HttpResponse deleteResponse = executeWebhookRequest("DELETE", deleteEndpoint);
                if (deleteResponse == null) {
                    failureCount++;
                    continue;
                }
                if (deleteResponse.statusCode() < 200 || deleteResponse.statusCode() >= 300) {
                    failureCount++;
                    plugin.getLogger().warning("Failed to delete asteroid webhook message during startup cleanup."
                            + " messageId=" + messageId
                            + " code=" + deleteResponse.statusCode()
                            + " body=" + safeValue(deleteResponse.body()));
                    continue;
                }
                deletedCount++;
            }

            if (messageIds.size() < WEBHOOK_PAGE_SIZE) {
                break;
            }
        }

        plugin.getLogger().info("Startup asteroid webhook cleanup completed. deletedMessages=" + deletedCount
                + " failures=" + failureCount + " pageCap=" + WEBHOOK_DELETE_PAGE_CAP);
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
                String responseBody = readResponseBody(connection, code);
                plugin.getLogger().warning("Discord webhook responded with non-2xx status. code=" + code
                        + " body=" + responseBody);
            } else {
                String responseBody = readResponseBody(connection, code);
                plugin.getLogger().info("Discord webhook responded. code=" + code + " body=" + responseBody);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deliver Discord webhook.", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String readResponseBody(HttpURLConnection connection, int code) throws IOException {
        InputStream stream = code >= 200 && code < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        if (stream == null) {
            return "";
        }
        try (InputStream responseStream = stream) {
            byte[] bytes = responseStream.readAllBytes();
            if (bytes.length == 0) {
                return "";
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private String resolveWebhookMessagesEndpoint(String webhookUrl) {
        URI uri;
        try {
            uri = URI.create(webhookUrl);
        } catch (IllegalArgumentException e) {
            return null;
        }

        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split("/");
        int webhooksIndex = -1;
        for (int i = 0; i < segments.length; i++) {
            if ("webhooks".equals(segments[i])) {
                webhooksIndex = i;
                break;
            }
        }
        if (webhooksIndex < 0 || webhooksIndex + 2 >= segments.length) {
            return null;
        }

        String webhookId = segments[webhooksIndex + 1];
        String webhookToken = segments[webhooksIndex + 2];
        if (webhookId.isBlank() || webhookToken.isBlank()) {
            return null;
        }

        String scheme = uri.getScheme() == null ? "https" : uri.getScheme();
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            return null;
        }
        int port = uri.getPort();
        String authority = port > 0 ? host + ":" + port : host;

        return scheme + "://" + authority + "/api/webhooks/" + webhookId + "/" + webhookToken + "/messages";
    }

    private List<String> extractMessageIds(String body) {
        List<String> ids = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return ids;
        }

        Matcher matcher = MESSAGE_ID_PATTERN.matcher(body);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    private HttpResponse executeWebhookRequest(String method, String endpoint) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(endpoint).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);

            int code = connection.getResponseCode();
            String responseBody = readResponseBody(connection, code);
            return new HttpResponse(code, responseBody);
        } catch (IOException | IllegalArgumentException e) {
            plugin.getLogger().log(Level.WARNING, "Discord webhook request failed. method=" + method
                    + " endpoint=" + endpoint, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private record HttpResponse(int statusCode, String body) {
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

    private String formatWorld(Location location) {
        if (location == null || location.getWorld() == null) {
            return "Unknown";
        }
        return location.getWorld().getName();
    }

    private String formatCoordinates(Location location) {
        if (location == null) {
            return "Unknown";
        }
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
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
