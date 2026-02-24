package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.entity.Player;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DialogueEngine {
    public enum LineType {TAUNT, BETRAYAL, PROMOTION, RETURN}

    private final LastBreathHC plugin;
    private final boolean enabled;
    private final long cooldownMs;
    private final int recentSuppression;
    private final Map<UUID, Long> perPlayerCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> perPlayerRecent = new ConcurrentHashMap<>();

    public DialogueEngine(LastBreathHC plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("nemesis.dialogue.enabled", true);
        this.cooldownMs = Math.max(0L, plugin.getConfig().getLong("nemesis.dialogue.cooldownMs", 6000L));
        this.recentSuppression = Math.max(1, plugin.getConfig().getInt("nemesis.dialogue.recentSuppression", 3));
    }

    public void emit(Player player, CaptainRecord speaker, LineType type) {
        if (!enabled || player == null || speaker == null || speaker.naming() == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now < perPlayerCooldown.getOrDefault(player.getUniqueId(), 0L)) {
            return;
        }
        String line = pickWeighted(type, player.getUniqueId(), speaker.naming().displayName());
        if (line == null || line.isBlank()) {
            return;
        }
        perPlayerCooldown.put(player.getUniqueId(), now + cooldownMs);
        player.sendMessage("§8[§cNemesis§8] §7" + line);
    }

    private String pickWeighted(LineType type, UUID playerId, String name) {
        List<String> pool = new ArrayList<>(switch (type) {
            case BETRAYAL -> List.of(name + " betrayed their warband.", name + " broke the oath.");
            case PROMOTION -> List.of(name + " rises in rank.", name + " claims new power.");
            case RETURN -> List.of(name + " returns from defeat.", name + " crawls back for revenge.");
            default -> List.of(name + ": You are marked.", name + ": I remember your fear.");
        });

        Deque<String> recent = perPlayerRecent.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        pool.removeIf(recent::contains);
        if (pool.isEmpty()) {
            return null;
        }
        String selected = pool.get((int) (Math.random() * pool.size()));
        recent.addLast(selected);
        while (recent.size() > recentSuppression) {
            recent.removeFirst();
        }
        return selected;
    }
}

