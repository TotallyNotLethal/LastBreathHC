package com.lastbreath.hc.lastBreathHC.integrations.lastbreath;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.Closeable;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiClient implements Closeable {
    private static final int MAX_ATTEMPTS = 3;
    private static final long BASE_BACKOFF_MILLIS = 500L;

    private final JavaPlugin plugin;
    private final HttpClient httpClient;
    private final ScheduledExecutorService scheduler;
    private final String baseUrl;
    private final String apiKey;

    public ApiClient(JavaPlugin plugin, String baseUrl, String apiKey) {
        this.plugin = plugin;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.apiKey = apiKey;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "lastbreath-api-client");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(scheduler)
                .build();
    }

    public void sendJoin(UUID uuid, String username) {
        postEvent("{\"event\":\"join\",\"uuid\":\"" + uuid + "\",\"username\":\"" + escapeJson(username) + "\"}");
    }

    public void sendLeave(UUID uuid) {
        postEvent("{\"event\":\"leave\",\"uuid\":\"" + uuid + "\"}");
    }

    public void sendDeath(UUID uuid, String deathMessage) {
        postEvent("{\"event\":\"death\",\"uuid\":\"" + uuid + "\",\"death_message\":\"" + escapeJson(deathMessage) + "\"}");
    }

    public void sendStats(UUID uuid, long survivalMinutes, int kills) {
        postEvent("{\"event\":\"stats\",\"uuid\":\"" + uuid + "\",\"survival_time\":" + survivalMinutes + ",\"kills\":" + kills + "}");
    }

    public void sendDragon(Optional<UUID> uuid) {
        String payload = uuid
                .map(value -> "{\"event\":\"dragon\",\"uuid\":\"" + value + "\"}")
                .orElse("{\"event\":\"dragon\"}");
        postEvent(payload);
    }

    private void postEvent(String payload) {
        postWithRetry("/plugin/event", payload, 1);
    }

    private void postWithRetry(String endpoint, String payload, int attempt) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + endpoint))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .header("x-api-key", apiKey)
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((response, throwable) -> {
                    if (throwable != null) {
                        handleFailure(endpoint, payload, attempt, "request error: " + throwable.getMessage());
                        return;
                    }

                    int statusCode = response.statusCode();
                    if (statusCode < 200 || statusCode >= 300) {
                        String responseBody = response.body() == null ? "" : response.body();
                        plugin.getLogger().warning("LastBreath API non-2xx response for " + endpoint
                                + " status=" + statusCode
                                + " body=" + responseBody);
                        handleFailure(endpoint, payload, attempt, "status " + statusCode);
                    }
                });
    }

    private void handleFailure(String endpoint, String payload, int attempt, String reason) {
        if (attempt >= MAX_ATTEMPTS) {
            plugin.getLogger().warning("LastBreath API request failed after " + attempt + " attempts for " + endpoint + ": " + reason);
            return;
        }

        long delayMillis = BASE_BACKOFF_MILLIS * (1L << (attempt - 1));
        scheduler.schedule(() -> postWithRetry(endpoint, payload, attempt + 1), delayMillis, TimeUnit.MILLISECONDS);
    }

    private static String stripTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private static String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }
}
