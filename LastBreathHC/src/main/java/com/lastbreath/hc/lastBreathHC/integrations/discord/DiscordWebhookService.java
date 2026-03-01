package com.lastbreath.hc.lastBreathHC.integrations.discord;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidManager;
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
    private static final int WEBHOOK_CLEANUP_PAGE_SIZE = 100;
    private static final int WEBHOOK_CLEANUP_MAX_PAGES = 20;
    private static final Pattern WEBHOOK_PATH_PATTERN = Pattern.compile(".*/webhooks/([0-9]+)/([^/?#]+).*");
    private static final Pattern MESSAGE_ID_PATTERN = Pattern.compile("\"id\"\\s*:\\s*\"(\\d+)\"");
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

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> postPayload(webhookUrl, payload, false));
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String responseBody = postPayload(webhookUrl, payload, true);
            String messageId = extractMessageId(responseBody);
            if (messageId == null || messageId.isBlank()) {
                plugin.getLogger().warning("Unable to resolve Discord message id for asteroid webhook at "
                        + formatLocation(loc) + ".");
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> AsteroidManager.updateDiscordMessageId(loc, messageId));
        });
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
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String responseBody = postPayload(webhookUrl, payload, true);
            String messageId = extractMessageId(responseBody);
            if (messageId == null || messageId.isBlank()) {
                plugin.getLogger().warning("Unable to resolve Discord message id for meteor shower webhook.");
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (AsteroidSpawnInfo asteroid : asteroids) {
                    AsteroidManager.updateDiscordMessageId(asteroid.location(), messageId);
                }
            });
        });
    }

    private String postPayload(String webhookUrl, Map<String, Object> payload, boolean includeWaitParam) {
        HttpURLConnection connection = null;
        try {
            String targetWebhookUrl = includeWaitParam ? withWaitParam(webhookUrl) : webhookUrl;
            connection = (HttpURLConnection) URI.create(targetWebhookUrl).toURL().openConnection();
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
                return null;
            }
            String responseBody = readResponseBody(connection, code);
            plugin.getLogger().info("Discord webhook responded. code=" + code + " body=" + responseBody);
            return responseBody;
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to deliver Discord webhook.", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void deleteWebhookMessage(String webhookUrl, String messageId) {
        if (webhookUrl == null || webhookUrl.isBlank() || messageId == null || messageId.isBlank()) {
            return;
        }

        WebhookCredentials credentials = resolveWebhookCredentials(webhookUrl);
        if (credentials == null) {
            plugin.getLogger().warning("Unable to resolve webhook credentials from URL; skipping delete for messageId="
                    + messageId);
            return;
        }
        deleteWebhookMessage(credentials, messageId);
    }

    public void clearAsteroidWebhookMessages() {
        if (!plugin.getConfig().getBoolean(CONFIG_ROOT + ".clearAsteroidMessagesOnStartup", true)) {
            plugin.getLogger().info("Asteroid webhook startup cleanup disabled by config.");
            return;
        }

        String webhookUrl = plugin.getConfig().getString(CONFIG_ROOT + ".asteroidsUrl", "").trim();
        if (webhookUrl.isEmpty()) {
            plugin.getLogger().info("Asteroid webhook URL empty; skipping startup cleanup.");
            return;
        }

        WebhookCredentials credentials = resolveWebhookCredentials(webhookUrl);
        if (credentials == null) {
            plugin.getLogger().warning("Unable to parse asteroid webhook URL for startup cleanup.");
            return;
        }

        int deletedCount = 0;
        int failedCount = 0;
        int pageCount = 0;
        String beforeMessageId = null;
        boolean reachedSafetyCap = false;

        while (pageCount < WEBHOOK_CLEANUP_MAX_PAGES) {
            String response = fetchWebhookMessages(credentials, beforeMessageId, WEBHOOK_CLEANUP_PAGE_SIZE);
            if (response == null) {
                failedCount++;
                break;
            }

            List<String> messageIds = extractMessageIdsFromMessagesResponse(response);
            if (messageIds.isEmpty()) {
                break;
            }

            for (String messageId : messageIds) {
                if (deleteWebhookMessage(credentials, messageId)) {
                    deletedCount++;
                } else {
                    failedCount++;
                }
            }

            pageCount++;
            beforeMessageId = messageIds.get(messageIds.size() - 1);
            if (messageIds.size() < WEBHOOK_CLEANUP_PAGE_SIZE) {
                break;
            }
        }

        if (pageCount >= WEBHOOK_CLEANUP_MAX_PAGES) {
            reachedSafetyCap = true;
        }

        plugin.getLogger().info("Asteroid webhook startup cleanup finished. deleted=" + deletedCount
                + " failures=" + failedCount + " pages=" + pageCount
                + (reachedSafetyCap ? " safetyCapReached=true" : ""));
    }

    private String fetchWebhookMessages(WebhookCredentials credentials, String beforeMessageId, int limit) {
        HttpURLConnection connection = null;
        try {
            StringBuilder targetUrl = new StringBuilder(credentials.baseUrl())
                    .append("/messages?limit=")
                    .append(limit);
            if (beforeMessageId != null && !beforeMessageId.isBlank()) {
                targetUrl.append("&before=").append(beforeMessageId);
            }
            connection = (HttpURLConnection) URI.create(targetUrl.toString()).toURL().openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);

            int code = connection.getResponseCode();
            String responseBody = readResponseBody(connection, code);
            if (code < 200 || code >= 300) {
                plugin.getLogger().warning("Discord webhook message fetch failed. code=" + code
                        + " body=" + responseBody);
                return null;
            }
            return responseBody;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to fetch Discord webhook messages for startup cleanup.", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private boolean deleteWebhookMessage(WebhookCredentials credentials, String messageId) {
        HttpURLConnection connection = null;
        try {
            String targetUrl = credentials.baseUrl() + "/messages/" + messageId;
            connection = (HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
            connection.setRequestMethod("DELETE");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(8000);

            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) {
                String responseBody = readResponseBody(connection, code);
                plugin.getLogger().warning("Discord webhook message delete failed. code=" + code
                        + " messageId=" + messageId + " body=" + responseBody);
                return false;
            }
            return true;
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to delete Discord webhook message. messageId=" + messageId, e);
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private WebhookCredentials resolveWebhookCredentials(String webhookUrl) {
        try {
            String sanitized = webhookUrl;
            int queryStart = sanitized.indexOf('?');
            if (queryStart >= 0) {
                sanitized = sanitized.substring(0, queryStart);
            }

            Matcher matcher = WEBHOOK_PATH_PATTERN.matcher(sanitized);
            if (!matcher.matches()) {
                return null;
            }

            String webhookId = matcher.group(1);
            String token = matcher.group(2);
            String baseUrl = sanitized.substring(0, matcher.start(1)) + webhookId + "/" + token;
            return new WebhookCredentials(baseUrl);
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<String> extractMessageIdsFromMessagesResponse(String responseBody) {
        List<String> ids = new ArrayList<>();
        if (responseBody == null || responseBody.isBlank()) {
            return ids;
        }

        boolean inString = false;
        boolean escaping = false;
        int arrayDepth = 0;
        int depth = 0;
        int objectStart = -1;

        for (int i = 0; i < responseBody.length(); i++) {
            char c = responseBody.charAt(i);
            if (inString) {
                if (escaping) {
                    escaping = false;
                    continue;
                }
                if (c == '\\') {
                    escaping = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }

            if (c == '[') {
                arrayDepth++;
                continue;
            }
            if (c == ']') {
                arrayDepth = Math.max(0, arrayDepth - 1);
                continue;
            }

            if (c == '{' && arrayDepth == 1) {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
                continue;
            }

            if (c == '}' && arrayDepth == 1 && depth > 0) {
                depth--;
                if (depth == 0) {
                    String objectBody = responseBody.substring(objectStart, i + 1);
                    Matcher matcher = MESSAGE_ID_PATTERN.matcher(objectBody);
                    if (matcher.find()) {
                        ids.add(matcher.group(1));
                    }
                    objectStart = -1;
                }
            }
        }
        return ids;
    }

    private record WebhookCredentials(String baseUrl) {
    }

    private String withWaitParam(String webhookUrl) {
        if (webhookUrl.contains("?")) {
            return webhookUrl + "&wait=true";
        }
        return webhookUrl + "?wait=true";
    }

    private String extractMessageId(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Matcher matcher = MESSAGE_ID_PATTERN.matcher(responseBody);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
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
