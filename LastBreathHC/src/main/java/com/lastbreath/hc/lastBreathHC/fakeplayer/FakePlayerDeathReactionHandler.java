package com.lastbreath.hc.lastBreathHC.fakeplayer;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FakePlayerDeathReactionHandler {
    private static final String CONFIG_PATH = "fakePlayers.deathReactions";

    private final LastBreathHC plugin;
    private final FakePlayerService fakePlayerService;
    private final Random random = new Random();
    private final Deque<Instant> recentReactionTimes = new ArrayDeque<>();
    private final Map<UUID, Instant> cooldownUntil = new ConcurrentHashMap<>();

    private int pendingReactions;

    public FakePlayerDeathReactionHandler(LastBreathHC plugin, FakePlayerService fakePlayerService) {
        this.plugin = plugin;
        this.fakePlayerService = fakePlayerService;
    }

    public void handlePlayerDeath(Player deadPlayer) {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(CONFIG_PATH);
        if (section == null || !section.getBoolean("enabled", true)) {
            return;
        }

        List<String> messages = section.getStringList("messages");
        if (messages.isEmpty()) {
            return;
        }

        int maxPerMinute = Math.max(0, section.getInt("maxReactionsPerMinute", 3));
        int configuredReactionCount = Math.max(1, section.getInt("reactionCount", 1));
        int availableSlots = getAvailableSlots(maxPerMinute);
        if (availableSlots <= 0) {
            return;
        }

        long cooldownSeconds = Math.max(0L, section.getLong("perFakeCooldownSeconds", 30L));
        int minDelayTicks = Math.max(0, section.getInt("delayTicks.min", 20));
        int maxDelayTicks = Math.max(minDelayTicks, section.getInt("delayTicks.max", 80));

        List<FakePlayerRecord> eligible = collectEligibleFakePlayers(Instant.now());
        if (eligible.isEmpty()) {
            return;
        }

        Collections.shuffle(eligible, random);
        int selectedCount = Math.min(configuredReactionCount, Math.min(availableSlots, eligible.size()));
        Instant cooldownExpiry = Instant.now().plusSeconds(cooldownSeconds);

        for (int index = 0; index < selectedCount; index++) {
            FakePlayerRecord selected = eligible.get(index);
            cooldownUntil.put(selected.getUuid(), cooldownExpiry);
            reserveReactionSlot();

            String message = messages.get(random.nextInt(messages.size()));
            int delay = randomDelay(minDelayTicks, maxDelayTicks);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                try {
                    dispatchReaction(selected.getUuid(), message);
                } finally {
                    releasePendingReservation();
                }
            }, delay);
        }

        plugin.getLogger().fine("Queued " + selectedCount + " fake-player death reaction(s) for " + deadPlayer.getName() + ".");
    }

    private void dispatchReaction(UUID fakePlayerUuid, String message) {
        FakePlayerRecord current = fakePlayerService.getByUuid(fakePlayerUuid).orElse(null);
        if (current == null || !current.isActive() || current.isMuted()) {
            return;
        }

        if (!fakePlayerService.registerChat(fakePlayerUuid, message)) {
            return;
        }

        fakePlayerService.registerReaction(fakePlayerUuid);
        recordReactionSend();
        Bukkit.broadcastMessage("<" + current.getName() + "> " + message);
    }

    private List<FakePlayerRecord> collectEligibleFakePlayers(Instant now) {
        List<FakePlayerRecord> eligible = new ArrayList<>();
        for (FakePlayerRecord record : fakePlayerService.listFakePlayers()) {
            if (!record.isActive() || record.isMuted()) {
                continue;
            }

            Instant until = cooldownUntil.get(record.getUuid());
            if (until != null && until.isAfter(now)) {
                continue;
            }
            eligible.add(record);
        }
        return eligible;
    }

    private int randomDelay(int minDelayTicks, int maxDelayTicks) {
        if (maxDelayTicks <= minDelayTicks) {
            return minDelayTicks;
        }
        return random.nextInt((maxDelayTicks - minDelayTicks) + 1) + minDelayTicks;
    }

    private synchronized int getAvailableSlots(int maxPerMinute) {
        pruneOldReactions();
        return Math.max(0, maxPerMinute - (recentReactionTimes.size() + pendingReactions));
    }

    private synchronized void reserveReactionSlot() {
        pendingReactions++;
    }

    private synchronized void releasePendingReservation() {
        pendingReactions = Math.max(0, pendingReactions - 1);
    }

    private synchronized void recordReactionSend() {
        recentReactionTimes.addLast(Instant.now());
        pruneOldReactions();
    }

    private void pruneOldReactions() {
        Instant cutoff = Instant.now().minus(Duration.ofMinutes(1));
        while (!recentReactionTimes.isEmpty() && recentReactionTimes.peekFirst().isBefore(cutoff)) {
            recentReactionTimes.removeFirst();
        }
    }
}
