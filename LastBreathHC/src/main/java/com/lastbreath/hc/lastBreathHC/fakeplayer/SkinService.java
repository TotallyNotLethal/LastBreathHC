package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SkinService {
    private static final Duration CACHE_TTL = Duration.ofHours(6);
    private static final Pattern JSON_STRING = Pattern.compile("\\\"([^\\\"]+)\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"");
    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            "\\\"name\\\"\\s*:\\s*\\\"textures\\\".*?\\\"value\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"(?:.*?\\\"signature\\\"\\s*:\\s*\\\"([^\\\"]+)\\\")?",
            Pattern.DOTALL
    );

    private final LastBreathHC plugin;
    private final HttpClient client;
    private final Map<String, SkinCacheEntry> cache = new ConcurrentHashMap<>();

    public SkinService(LastBreathHC plugin) {
        this.plugin = plugin;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    }

    public void loadCache(Map<String, SkinCacheEntry> loaded) {
        cache.clear();
        cache.putAll(loaded);
    }

    public Map<String, SkinCacheEntry> snapshotCache() {
        return Map.copyOf(cache);
    }

    public SkinLookupResult lookup(String owner, boolean forceRefresh) {
        String key = normalizeOwner(owner);
        if (key == null) {
            return SkinLookupResult.empty();
        }

        SkinCacheEntry cached = cache.get(key);
        Instant now = Instant.now();
        if (!forceRefresh && isValid(cached, now)) {
            return SkinLookupResult.fromCache(cached);
        }

        try {
            SkinCacheEntry refreshed = fetchFromApi(key, now);
            cache.put(key, refreshed);
            return SkinLookupResult.refreshed(refreshed);
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to refresh skin for owner '" + key + "': " + ex.getMessage());
            if (cached != null && hasSkin(cached)) {
                return SkinLookupResult.refreshFailed(cached);
            }
            return SkinLookupResult.empty();
        }
    }

    private SkinCacheEntry fetchFromApi(String owner, Instant now) throws IOException, InterruptedException {
        String profileUrl = "https://api.mojang.com/users/profiles/minecraft/" + URLEncoder.encode(owner, StandardCharsets.UTF_8);
        String profileBody = httpGet(profileUrl);
        String profileId = jsonValue(profileBody, "id").orElseThrow(() -> new IOException("Missing Mojang profile id"));

        String sessionUrl = "https://sessionserver.mojang.com/session/minecraft/profile/" + profileId + "?unsigned=false";
        String sessionBody = httpGet(sessionUrl);
        Matcher propertyMatcher = PROPERTY_PATTERN.matcher(sessionBody);
        if (!propertyMatcher.find()) {
            throw new IOException("Missing textures property");
        }

        String textures = propertyMatcher.group(1);
        String signature = propertyMatcher.group(2);
        return new SkinCacheEntry(owner, textures, signature, now, now.plus(CACHE_TTL));
    }

    private String httpGet(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(6))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode() + " from " + url);
        }
        return response.body();
    }

    private Optional<String> jsonValue(String json, String key) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        Matcher matcher = JSON_STRING.matcher(json);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) {
                return Optional.ofNullable(matcher.group(2));
            }
        }
        return Optional.empty();
    }

    private boolean isValid(SkinCacheEntry entry, Instant now) {
        return entry != null
                && hasSkin(entry)
                && entry.getExpiresAt() != null
                && entry.getExpiresAt().isAfter(now);
    }

    private boolean hasSkin(SkinCacheEntry entry) {
        return entry.getTextures() != null && !entry.getTextures().isBlank();
    }

    private String normalizeOwner(String owner) {
        if (owner == null) {
            return null;
        }
        String normalized = owner.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public record SkinLookupResult(String textures, String signature, boolean refreshed, boolean usedCachedAfterFailure) {
        private static SkinLookupResult fromCache(SkinCacheEntry entry) {
            return new SkinLookupResult(entry.getTextures(), entry.getSignature(), false, false);
        }

        private static SkinLookupResult refreshed(SkinCacheEntry entry) {
            return new SkinLookupResult(entry.getTextures(), entry.getSignature(), true, false);
        }

        private static SkinLookupResult refreshFailed(SkinCacheEntry entry) {
            return new SkinLookupResult(entry.getTextures(), entry.getSignature(), false, true);
        }

        private static SkinLookupResult empty() {
            return new SkinLookupResult(null, null, false, false);
        }

        public boolean hasSkin() {
            return textures != null && !textures.isBlank();
        }
    }
}

