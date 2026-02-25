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

    public record DialogueExchange(String line, double weight, ActionType actionType, DialogueExchange reply,
                                   List<DialogueExchange> followUps) {
    }

    @FunctionalInterface
    public interface DialogueActionHook {
        void onAction(ActionType actionType, CaptainRecord speaker, CaptainRecord listener, String channelKey, Location location);
    }

    private static final int MAX_EXCHANGE_DEPTH = 3;

    private final LastBreathHC plugin;
    private final boolean enabled;
    private final long cooldownMs;
    private final int recentSuppression;
    private final int conversationRecentSuppression;
    private final double audienceRangeMeters;
    private final long npcConversationCooldownMs;
    private final long responseDelayMinTicks;
    private final long responseDelayMaxTicks;
    private final Map<UUID, Long> perPlayerCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Deque<String>> perPlayerRecent = new ConcurrentHashMap<>();
    private final Map<Tone, List<DialogueOption>> choiceOptions = new EnumMap<>(Tone.class);
    private final Map<String, List<DialogueExchange>> npcConversationOptions = new HashMap<>();
    private final Map<String, List<DialogueExchange>> eventConversationOptions = new HashMap<>();
    private final Map<String, Long> npcConversationCooldowns = new ConcurrentHashMap<>();
    private final Map<String, Deque<String>> pairRecentNpc = new ConcurrentHashMap<>();
    private final Map<String, Deque<String>> pairRecentEvent = new ConcurrentHashMap<>();
    private volatile DialogueActionHook actionHook = (actionType, speaker, listener, channelKey, location) -> {
    };

    public DialogueEngine(LastBreathHC plugin) {
        this.plugin = plugin;
        this.enabled = plugin.getConfig().getBoolean("nemesis.dialogue.enabled", true);
        this.cooldownMs = Math.max(0L, plugin.getConfig().getLong("nemesis.dialogue.cooldownMs", 6000L));
        this.recentSuppression = Math.max(1, plugin.getConfig().getInt("nemesis.dialogue.recentSuppression", 3));
        this.conversationRecentSuppression = Math.max(1, plugin.getConfig().getInt("nemesis.dialogue.conversationRecentSuppression", 2));
        this.audienceRangeMeters = Math.max(5.0, plugin.getConfig().getDouble("nemesis.dialogue.audienceRangeMeters", 50.0));
        this.npcConversationCooldownMs = Math.max(2000L, plugin.getConfig().getLong("nemesis.dialogue.npcConversationCooldownMs", 15000L));
        long configuredResponseDelayMin = Math.max(0L, plugin.getConfig().getLong("nemesis.dialogue.responseDelayMinTicks", 12L));
        long configuredResponseDelayMax = Math.max(0L, plugin.getConfig().getLong("nemesis.dialogue.responseDelayMaxTicks", 36L));
        this.responseDelayMinTicks = Math.min(configuredResponseDelayMin, configuredResponseDelayMax);
        this.responseDelayMaxTicks = Math.max(configuredResponseDelayMin, configuredResponseDelayMax);
        loadDialogueChoices();
    }

    public void setActionHook(DialogueActionHook actionHook) {
        this.actionHook = actionHook == null ? (actionType, speaker, listener, channelKey, location) -> {
        } : actionHook;
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
        String channelKey = speakerRank.name().toLowerCase(Locale.ROOT) + "_to_" + listenerRank.name().toLowerCase(Locale.ROOT);
        List<DialogueExchange> options = npcConversationOptions.getOrDefault(channelKey, List.of());
        if (options.isEmpty()) {
            return false;
        }
        Deque<String> recent = pairRecentNpc.computeIfAbsent(pairKey, ignored -> new ArrayDeque<>());
        DialogueExchange chosen = weightedExchange(options, recent);
        if (chosen == null || chosen.line() == null || chosen.line().isBlank()) {
            return false;
        }

        emitExchange(channelKey, chosen, speaker, listener, location, true, 0);
        npcConversationCooldowns.put(pairKey, now + npcConversationCooldownMs);
        return true;
    }

    public boolean emitEventConversation(String key, CaptainRecord speaker, CaptainRecord listener, String fallenName, Location location) {
        if (!enabled || key == null || speaker == null || location == null || speaker.naming() == null) {
            return false;
        }
        String channelKey = key.toLowerCase(Locale.ROOT);
        List<DialogueExchange> options = eventConversationOptions.getOrDefault(channelKey, List.of());
        if (options.isEmpty()) {
            return false;
        }
        String pairKey = speaker.identity() == null ? "unknown" : pairKey(speaker.identity().captainId(), listener != null && listener.identity() != null ? listener.identity().captainId() : speaker.identity().captainId());
        Deque<String> recent = pairRecentEvent.computeIfAbsent(channelKey + ":" + pairKey, ignored -> new ArrayDeque<>());
        DialogueExchange chosen = weightedExchange(options, recent);
        if (chosen == null || chosen.line() == null || chosen.line().isBlank()) {
            return false;
        }

        emitExchange(channelKey, chosen, speaker, listener, location, false, 0, fallenName);
        return true;
    }

    public double audienceRangeMeters() {
        return audienceRangeMeters;
    }

    private void emitExchange(String channelKey, DialogueExchange exchange, CaptainRecord speaker, CaptainRecord listener, Location location, boolean npcConversation, int depth) {
        emitExchange(channelKey, exchange, speaker, listener, location, npcConversation, depth, "");
    }

    private void emitExchange(String channelKey, DialogueExchange exchange, CaptainRecord speaker, CaptainRecord listener, Location location,
                              boolean npcConversation, int depth, String fallenName) {
        if (exchange == null || depth > MAX_EXCHANGE_DEPTH) {
            return;
        }
        String speakerName = speaker != null && speaker.naming() != null ? speaker.naming().displayName() : "Unknown";
        String listenerName = listener != null && listener.naming() != null ? listener.naming().displayName() : "their foe";

        String rendered = renderLine(exchange.line(), speakerName, listenerName, fallenName);
        if (!rendered.isBlank()) {
            String prefix = npcConversation
                    ? "§8[§4Warband§8] §6" + speakerName + " §7to §c" + listenerName + "§7: "
                    : "§8[§4Warband§8] §c" + speakerName + "§7: ";
            broadcastToNearby(location, prefix + rendered);
        }
        applyAction(exchange.actionType(), speaker, listener, channelKey, location);

        if (exchange.reply() != null) {
            DialogueExchange reply = exchange.reply();
            runLater(randomResponseDelayTicks(), () -> {
                String replyRendered = renderLine(reply.line(), listenerName, speakerName, fallenName);
                if (!replyRendered.isBlank()) {
                    String replyPrefix = npcConversation
                            ? "§8[§4Warband§8] §6" + listenerName + " §7to §c" + speakerName + "§7: "
                            : "§8[§4Warband§8] §c" + listenerName + "§7: ";
                    broadcastToNearby(location, replyPrefix + replyRendered);
                }
                applyAction(reply.actionType(), listener, speaker, channelKey, location);
                emitFollowUp(channelKey, exchange, speaker, listener, location, npcConversation, depth, fallenName);
            });
            return;
        }

        emitFollowUp(channelKey, exchange, speaker, listener, location, npcConversation, depth, fallenName);
    }

    private void emitFollowUp(String channelKey, DialogueExchange exchange, CaptainRecord speaker, CaptainRecord listener, Location location,
                              boolean npcConversation, int depth, String fallenName) {
        if (exchange.followUps() == null || exchange.followUps().isEmpty()) {
            return;
        }
        DialogueExchange followUp = weightedExchange(exchange.followUps(), null);
        if (followUp == null) {
            return;
        }
        runLater(randomResponseDelayTicks(), () -> emitExchange(channelKey, followUp, speaker, listener, location, npcConversation, depth + 1, fallenName));
    }

    private String renderLine(String line, String speakerName, String listenerName, String fallenName) {
        if (line == null) {
            return "";
        }
        return line
                .replace("{speaker}", speakerName)
                .replace("{listener}", listenerName)
                .replace("{fallen}", fallenName == null || fallenName.isBlank() ? "their blood brother" : fallenName)
                .trim();
    }

    private void applyAction(ActionType actionType, CaptainRecord speaker, CaptainRecord listener, String channelKey, Location location) {
        if (actionType == null) {
            return;
        }
        actionHook.onAction(actionType, speaker, listener, channelKey, location);
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

    private long randomResponseDelayTicks() {
        if (responseDelayMaxTicks <= responseDelayMinTicks) {
            return responseDelayMinTicks;
        }
        return responseDelayMinTicks + (long) (Math.random() * (responseDelayMaxTicks - responseDelayMinTicks + 1));
    }

    private void runLater(long delayTicks, Runnable task) {
        if (task == null) {
            return;
        }
        if (delayTicks <= 0L) {
            task.run();
            return;
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks);
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

    private DialogueExchange weightedExchange(List<DialogueExchange> options, Deque<String> recent) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        List<DialogueExchange> filtered = new ArrayList<>(options);
        if (recent != null && !recent.isEmpty()) {
            filtered.removeIf(option -> recent.contains(signature(option)));
            if (filtered.isEmpty()) {
                filtered = new ArrayList<>(options);
            }
        }

        double total = filtered.stream().mapToDouble(DialogueExchange::weight).filter(weight -> weight > 0.0).sum();
        DialogueExchange chosen;
        if (total <= 0.0) {
            chosen = filtered.get((int) (Math.random() * filtered.size()));
        } else {
            double roll = Math.random() * total;
            double running = 0.0;
            chosen = filtered.get(filtered.size() - 1);
            for (DialogueExchange option : filtered) {
                running += Math.max(0.0, option.weight());
                if (roll <= running) {
                    chosen = option;
                    break;
                }
            }
        }

        if (recent != null && chosen != null) {
            recent.addLast(signature(chosen));
            while (recent.size() > conversationRecentSuppression) {
                recent.removeFirst();
            }
        }
        return chosen;
    }

    private String signature(DialogueExchange exchange) {
        if (exchange == null) {
            return "none";
        }
        String reply = exchange.reply() == null ? "" : exchange.reply().line();
        return (exchange.line() == null ? "" : exchange.line()) + "|" + reply;
    }

    private void loadDialogueChoices() {
        choiceOptions.clear();
        npcConversationOptions.clear();
        eventConversationOptions.clear();
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

        loadNpcSection(yaml.getConfigurationSection("npcConversations"), npcConversationOptions);
        loadNpcSection(yaml.getConfigurationSection("eventConversations"), eventConversationOptions);
    }

    private void loadNpcSection(ConfigurationSection section, Map<String, List<DialogueExchange>> sink) {
        if (section == null) {
            return;
        }
        for (String channel : section.getKeys(false)) {
            ConfigurationSection channelSection = section.getConfigurationSection(channel);
            if (channelSection == null) {
                continue;
            }
            List<DialogueExchange> lines = new ArrayList<>();
            for (String key : channelSection.getKeys(false)) {
                DialogueExchange exchange = parseExchange(channelSection.get(key), 0);
                if (exchange != null && exchange.line() != null && !exchange.line().isBlank()) {
                    lines.add(exchange);
                }
            }
            if (!lines.isEmpty()) {
                sink.put(channel.toLowerCase(Locale.ROOT), lines);
            }
        }
    }

    private DialogueExchange parseExchange(Object raw, int depth) {
        if (raw == null || depth > MAX_EXCHANGE_DEPTH) {
            return null;
        }
        if (raw instanceof String rawLine) {
            String line = rawLine.trim();
            return line.isBlank() ? null : new DialogueExchange(line, 1.0, null, null, List.of());
        }
        if (!(raw instanceof ConfigurationSection section)) {
            return null;
        }

        String line = section.getString("line", "").trim();
        if (line.isBlank()) {
            return null;
        }
        double weight = Math.max(0.0, section.getDouble("weight", 1.0));
        ActionType actionType = parseAction(section.getString("action", ""));

        DialogueExchange reply = parseExchange(section.get("reply"), depth + 1);

        List<DialogueExchange> followUps = new ArrayList<>();
        ConfigurationSection followUpSection = section.getConfigurationSection("followUps");
        if (followUpSection == null) {
            followUpSection = section.getConfigurationSection("followups");
        }
        if (followUpSection != null) {
            for (String key : followUpSection.getKeys(false)) {
                DialogueExchange followUp = parseExchange(followUpSection.get(key), depth + 1);
                if (followUp != null && followUp.line() != null && !followUp.line().isBlank()) {
                    followUps.add(followUp);
                }
            }
        }

        return new DialogueExchange(line, weight, actionType, reply, followUps);
    }

    private ActionType parseAction(String actionRaw) {
        if (actionRaw == null || actionRaw.isBlank()) {
            return null;
        }
        try {
            return ActionType.valueOf(actionRaw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
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
