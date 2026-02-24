package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DialogueEngine {
    public enum LineType {TAUNT, BETRAYAL, PROMOTION, RETURN}

    public enum Tone {POSITIVE, NEUTRAL, NEGATIVE}

    public enum ActionType {UNITY, FORTIFY, BETRAYAL, BLOOD_FEUD, AGGRESSION, STAND_DOWN}

    public record DialogueOption(String line, ActionType actionType, double weight) {
    }

    public record DialogueResolution(String spokenLine, ActionType actionType, Tone tone) {
    }

    public record NpcDialogueLine(String line, double weight) {
    }

    private final LastBreathHC plugin;
    private final boolean enabled;
    private final long cooldownMs;
    private final int recentSuppression;
    private final double audienceRangeMeters;
    private final long npcConversationCooldownMs;
    private final Map<UUID, Long> perPlayerCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> perPlayerRecent = new ConcurrentHashMap<>();
    private final Map<Tone, List<DialogueOption>> choiceOptions = new EnumMap<>(Tone.class);
    private final Map<String, List<NpcDialogueLine>> npcConversationOptions = new HashMap<>();
    private final Map<String, Long> npcConversationCooldowns = new ConcurrentHashMap<>();

    public DialogueEngine(LastBreathHC plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("nemesis.dialogue.enabled", true);
        this.cooldownMs = Math.max(0L, plugin.getConfig().getLong("nemesis.dialogue.cooldownMs", 6000L));
        this.recentSuppression = Math.max(1, plugin.getConfig().getInt("nemesis.dialogue.recentSuppression", 3));
        this.audienceRangeMeters = Math.max(5.0, plugin.getConfig().getDouble("nemesis.dialogue.audienceRangeMeters", 50.0));
        this.npcConversationCooldownMs = Math.max(2000L, plugin.getConfig().getLong("nemesis.dialogue.npcConversationCooldownMs", 15000L));
        loadDialogueChoices();
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

    public DialogueResolution resolveChoice(Player chooser, CaptainRecord speaker, String toneRaw) {
        if (!enabled || chooser == null || speaker == null || speaker.naming() == null) {
            return null;
        }
        Tone tone;
        try {
            tone = Tone.valueOf(toneRaw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return null;
        }

        List<DialogueOption> options = choiceOptions.getOrDefault(tone, List.of());
        if (options.isEmpty()) {
            return null;
        }
        DialogueOption chosen = weighted(options);
        if (chosen == null) {
            return null;
        }

        String rendered = chosen.line().replace("{captain}", speaker.naming().displayName()).replace("{player}", chooser.getName());
        broadcastToNearby(chooser.getLocation(), "§8[§cNemesis§8] §6" + speaker.naming().displayName() + "§7: " + rendered);
        return new DialogueResolution(rendered, chosen.actionType(), tone);
    }

    public boolean emitNpcConversation(CaptainRecord speaker, CaptainRecord listener, Location location) {
        if (!enabled || speaker == null || listener == null || location == null) {
            return false;
        }
        if (speaker.naming() == null || listener.naming() == null || speaker.identity() == null || listener.identity() == null) {
            return false;
        }
        String pairKey = pairKey(speaker.identity().captainId(), listener.identity().captainId());
        long now = System.currentTimeMillis();
        if (now < npcConversationCooldowns.getOrDefault(pairKey, 0L)) {
            return false;
        }

        Rank speakerRank = Rank.from(speaker.political().map(CaptainRecord.Political::rank).orElse("CAPTAIN"), Rank.CAPTAIN);
        Rank listenerRank = Rank.from(listener.political().map(CaptainRecord.Political::rank).orElse("CAPTAIN"), Rank.CAPTAIN);
        String key = speakerRank.name().toLowerCase(Locale.ROOT) + "_to_" + listenerRank.name().toLowerCase(Locale.ROOT);
        List<NpcDialogueLine> options = npcConversationOptions.getOrDefault(key, List.of());
        if (options.isEmpty()) {
            return false;
        }
        NpcDialogueLine chosen = weightedNpc(options);
        if (chosen == null || chosen.line() == null || chosen.line().isBlank()) {
            return false;
        }

        String rendered = chosen.line()
                .replace("{speaker}", speaker.naming().displayName())
                .replace("{listener}", listener.naming().displayName());
        broadcastToNearby(location, "§8[§4Warband§8] §6" + speaker.naming().displayName() + " §7to §c" + listener.naming().displayName() + "§7: " + rendered);
        npcConversationCooldowns.put(pairKey, now + npcConversationCooldownMs);
        return true;
    }

    public double audienceRangeMeters() {
        return audienceRangeMeters;
    }

    private String pairKey(UUID left, UUID right) {
        if (left == null || right == null) {
            return "unknown";
        }
        return left.compareTo(right) <= 0 ? left + ":" + right : right + ":" + left;
    }

    private void broadcastToNearby(Location origin, String message) {
        if (origin == null || origin.getWorld() == null) {
            return;
        }
        double maxDistanceSq = audienceRangeMeters * audienceRangeMeters;
        for (Player nearby : origin.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(origin) <= maxDistanceSq) {
                nearby.sendMessage(message);
            }
        }
    }

    private DialogueOption weighted(List<DialogueOption> options) {
        double total = options.stream().mapToDouble(DialogueOption::weight).filter(weight -> weight > 0.0).sum();
        if (total <= 0.0) {
            return options.get((int) (Math.random() * options.size()));
        }
        double roll = Math.random() * total;
        double running = 0.0;
        for (DialogueOption option : options) {
            running += Math.max(0.0, option.weight());
            if (roll <= running) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    private NpcDialogueLine weightedNpc(List<NpcDialogueLine> options) {
        double total = options.stream().mapToDouble(NpcDialogueLine::weight).filter(weight -> weight > 0.0).sum();
        if (total <= 0.0) {
            return options.get((int) (Math.random() * options.size()));
        }
        double roll = Math.random() * total;
        double running = 0.0;
        for (NpcDialogueLine option : options) {
            running += Math.max(0.0, option.weight());
            if (roll <= running) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    private void loadDialogueChoices() {
        choiceOptions.clear();
        npcConversationOptions.clear();
        for (Tone tone : Tone.values()) {
            choiceOptions.put(tone, new ArrayList<>());
        }

        File file = new File(plugin.getDataFolder(), "nemesis-dialogue.yml");
        if (!file.exists()) {
            plugin.saveResource("nemesis-dialogue.yml", false);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        for (Tone tone : Tone.values()) {
            ConfigurationSection section = yaml.getConfigurationSection("choices." + tone.name().toLowerCase(Locale.ROOT));
            if (section == null) {
                continue;
            }
            for (String key : section.getKeys(false)) {
                ConfigurationSection option = section.getConfigurationSection(key);
                if (option == null) {
                    continue;
                }
                String line = option.getString("line", "").trim();
                String actionRaw = option.getString("action", "STAND_DOWN");
                ActionType action;
                try {
                    action = ActionType.valueOf(actionRaw.trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    action = ActionType.STAND_DOWN;
                }
                double weight = Math.max(0.0, option.getDouble("weight", 1.0));
                if (!line.isBlank()) {
                    choiceOptions.get(tone).add(new DialogueOption(line, action, weight));
                }
            }
        }

        ConfigurationSection conversations = yaml.getConfigurationSection("npcConversations");
        if (conversations != null) {
            for (String channel : conversations.getKeys(false)) {
                ConfigurationSection channelSection = conversations.getConfigurationSection(channel);
                if (channelSection == null) {
                    continue;
                }
                List<NpcDialogueLine> lines = new ArrayList<>();
                for (String key : channelSection.getKeys(false)) {
                    ConfigurationSection option = channelSection.getConfigurationSection(key);
                    if (option == null) {
                        continue;
                    }
                    String line = option.getString("line", "").trim();
                    double weight = Math.max(0.0, option.getDouble("weight", 1.0));
                    if (!line.isBlank()) {
                        lines.add(new NpcDialogueLine(line, weight));
                    }
                }
                if (!lines.isEmpty()) {
                    npcConversationOptions.put(channel.toLowerCase(Locale.ROOT), lines);
                }
            }
        }
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
