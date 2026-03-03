package com.lastbreath.hc.lastBreathHC.integrations.lastbreath;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiClient implements Closeable {
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 500L;
    private static final int PAYLOAD_LOG_MAX_LENGTH = 300;

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final URI pluginEventUri;
    private final String apiKey;

    public ApiClient(JavaPlugin plugin, String baseUrl, String apiKey) {
        this.plugin = plugin;
        this.pluginEventUri = resolvePluginEventUri(baseUrl);
        this.apiKey = apiKey;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "lastbreath-api-client");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .executor(scheduler)
                .build();

        plugin.getLogger().info("LastBreath API client initialized. configuredBaseUrl=" + baseUrl
                + " resolvedPluginEventUrl=" + this.pluginEventUri);
    }

    public void sendJoin(UUID uuid, String username) {
        postEvent("join", "{\"event\":\"join\",\"uuid\":\"" + uuid + "\",\"username\":\"" + escapeJson(username) + "\"}");
    }

    public void sendLeave(UUID uuid) {
        postEvent("leave", "{\"event\":\"leave\",\"uuid\":\"" + uuid + "\"}");
    }

    public void sendDeath(UUID uuid, String deathMessage) {
        postEvent("death", "{\"event\":\"death\",\"uuid\":\"" + uuid + "\",\"death_message\":\"" + escapeJson(deathMessage) + "\"}");
    }

    public void sendStats(UUID uuid, long survivalMinutes, int kills) {
        postEvent("stats", "{\"event\":\"stats\",\"uuid\":\"" + uuid + "\",\"survival_time\":" + survivalMinutes + ",\"kills\":" + kills + "}");
    }

    public void sendBulkStats(Collection<PlayerStatsPayload> players) {
        List<PlayerStatsPayload> entries = new ArrayList<>(players);
        entries.sort((left, right) -> left.uuid().compareTo(right.uuid()));

        StringBuilder payload = new StringBuilder("{\"event\":\"bulk_stats\",\"players\":[");
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) {
                payload.append(',');
            }
            appendPlayerJson(payload, entries.get(i));
        }
        payload.append("]}");
        postEvent("bulk_stats", payload.toString());
    }

    public void sendDragon(Optional<UUID> uuid) {
        String payload = uuid
                .map(value -> "{\"event\":\"dragon\",\"uuid\":\"" + value + "\"}")
                .orElse("{\"event\":\"dragon\"}");
        postEvent("dragon", payload);
    }

    public String getResolvedPluginEventUrl() {
        return pluginEventUri.toString();
    }

    private void postEvent(String eventType, String payload) {
        postWithRetry(eventType, payload, 1);
    }

    private void postWithRetry(String eventType, String payload, int attempt) {
        String requestUrl = pluginEventUri.toString();
        plugin.getLogger().info("LastBreath API POST attempt=" + attempt
                + " url=" + requestUrl
                + " event=" + eventType
                + " payload=" + summarizePayload(payload));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(pluginEventUri)
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        handleFailure(eventType, payload, attempt, "request error: " + throwable.getMessage());
                        return;
                    }

                    int statusCode = response.statusCode();
                    String responseBody = response.body() == null ? "" : response.body();
                    if (statusCode >= 200 && statusCode < 300) {
                        plugin.getLogger().info("LastBreath API success"
                                + " attempt=" + attempt
                                + " url=" + requestUrl
                                + " event=" + eventType
                                + " status=" + statusCode
                                + " body=" + summarizePayload(responseBody));
                        return;
                    }

                    plugin.getLogger().warning("LastBreath API non-2xx response"
                            + " attempt=" + attempt
                            + " url=" + requestUrl
                            + " event=" + eventType
                            + " status=" + statusCode
                            + " body=" + summarizePayload(responseBody));

                    if (!shouldRetry(statusCode)) {
                        return;
                    }

                    handleFailure(eventType, payload, attempt, "status " + statusCode);
                });
    }

    private void handleFailure(String eventType, String payload, int attempt, String reason) {
        if (attempt >= MAX_ATTEMPTS) {
            plugin.getLogger().warning("LastBreath API request failed after " + attempt + " attempts"
                    + " url=" + pluginEventUri
                    + " event=" + eventType
                    + " reason=" + reason
                    + " payload=" + summarizePayload(payload));
            return;
        }

        long delayMillis = BASE_BACKOFF_MILLIS * (1L << (attempt - 1));
        int nextAttempt = attempt + 1;
        plugin.getLogger().warning("LastBreath API scheduling retry"
                + " url=" + pluginEventUri
                + " event=" + eventType
                + " reason=" + reason
                + " nextAttempt=" + nextAttempt
                + " delayMs=" + delayMillis);
        scheduler.schedule(() -> postWithRetry(eventType, payload, nextAttempt), delayMillis, TimeUnit.MILLISECONDS);
    }

    private static URI resolvePluginEventUri(String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim();
        if (normalized.isEmpty()) {
            normalized = "https://www.lastbreath.net";
        }

        normalized = normalizeKnownLastBreathHost(normalized);

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.endsWith("/api")) {
            normalized = normalized.substring(0, normalized.length() - 4);
        }

        return URI.create(normalized + "/api/plugin/event");
    }

    private static String normalizeKnownLastBreathHost(String baseUrl) {
        if (baseUrl.equalsIgnoreCase("https://lastbreath.net")) {
            return "https://www.lastbreath.net";
        }

        if (baseUrl.equalsIgnoreCase("http://lastbreath.net")) {
            return "http://www.lastbreath.net";
        }

        return baseUrl;
    }

    private static boolean shouldRetry(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }

    private static String summarizePayload(String value) {
        if (value == null) {
            return "<null>";
        }

        String sanitized = value
                .replace('\n', ' ')
                .replace('\r', ' ')
                .trim();

        if (sanitized.length() <= PAYLOAD_LOG_MAX_LENGTH) {
            return sanitized;
        }

        return sanitized.substring(0, PAYLOAD_LOG_MAX_LENGTH) + "...<truncated>";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }

        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    private static void appendPlayerJson(StringBuilder payload, PlayerStatsPayload player) {
        payload.append("{\"uuid\":\"").append(player.uuid())
                .append("\",\"username\":\"").append(escapeJson(player.username()))
                .append("\",\"nickname\":");

        if (player.nickname() == null || player.nickname().isBlank()) {
            payload.append("null");
        } else {
            payload.append("\"").append(escapeJson(player.nickname())).append("\"");
        }

        payload.append(",\"time_alive\":").append(player.timeAlive())
                .append(",\"deaths\":").append(player.deaths())
                .append(",\"revives\":").append(player.revives())
                .append(",\"mobs_killed\":").append(player.mobsKilled())
                .append(",\"asteroid_loots\":").append(player.asteroidLoots())
                .append(",\"crops_harvested\":").append(player.cropsHarvested())
                .append(",\"blocks_mined\":").append(player.blocksMined())
                .append(",\"blocks_placed\":").append(player.blocksPlaced())
                .append(",\"fish_caught\":").append(player.fishCaught())
                .append(",\"player_kills\":").append(player.playerKills())
                .append(",\"rare_ores_mined\":").append(player.rareOresMined())
                .append(",\"world_scaler_enabled\":").append(player.worldScalerEnabled())
                .append(",\"unlocked_titles\":");
        appendStringArray(payload, player.unlockedTitles());
        payload.append(",\"equipped_title\":");
        appendNullableString(payload, player.equippedTitle());
        payload.append(",\"unlocked_prefixes\":");
        appendStringArray(payload, player.unlockedPrefixes());
        payload.append(",\"equipped_prefix\":");
        appendNullableString(payload, player.equippedPrefix());
        payload.append(",\"unlocked_auras\":");
        appendStringArray(payload, player.unlockedAuras());
        payload.append(",\"equipped_aura\":");
        appendNullableString(payload, player.equippedAura());
        payload.append(",\"unlocked_kill_messages\":");
        appendStringArray(payload, player.unlockedKillMessages());
        payload.append(",\"equipped_kill_message\":");
        appendNullableString(payload, player.equippedKillMessage());
        payload.append(",\"is_alive\":").append(player.isAlive())
                .append(",\"is_banned\":").append(player.isBanned())
                .append('}');
    }

    private static void appendNullableString(StringBuilder payload, String value) {
        if (value == null || value.isBlank()) {
            payload.append("null");
            return;
        }

        payload.append("\"").append(escapeJson(value)).append("\"");
    }

    private static void appendStringArray(StringBuilder payload, List<String> values) {
        payload.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                payload.append(',');
            }
            payload.append("\"").append(escapeJson(values.get(i))).append("\"");
        }
        payload.append(']');
    }

    public record PlayerStatsPayload(
            UUID uuid,
            String username,
            String nickname,
            long timeAlive,
            int deaths,
            int revives,
            int mobsKilled,
            int asteroidLoots,
            int cropsHarvested,
            int blocksMined,
            int blocksPlaced,
            int fishCaught,
            int playerKills,
            int rareOresMined,
            boolean worldScalerEnabled,
            List<String> unlockedTitles,
            String equippedTitle,
            List<String> unlockedPrefixes,
            String equippedPrefix,
            List<String> unlockedAuras,
            String equippedAura,
            List<String> unlockedKillMessages,
            String equippedKillMessage,
            boolean isAlive,
            boolean isBanned
    ) {
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
