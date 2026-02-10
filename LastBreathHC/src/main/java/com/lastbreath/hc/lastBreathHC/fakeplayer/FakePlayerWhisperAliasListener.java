package com.lastbreath.hc.lastBreathHC.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Command-compatibility bridge for whisper aliases routed to visual fake players.
 */
public class FakePlayerWhisperAliasListener implements Listener {

    private static final Set<String> WHISPER_ALIASES = Set.of(
            "msg", "tell", "w", "whisper", "m",
            "pm", "dm", "privatemessage", "message"
    );

    private final FakePlayerService fakePlayerService;

    public FakePlayerWhisperAliasListener(FakePlayerService fakePlayerService) {
        this.fakePlayerService = fakePlayerService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onWhisperAlias(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) {
            return;
        }

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 3) {
            return;
        }

        String command = normalizeCommand(parts[0]);
        if (!WHISPER_ALIASES.contains(command)) {
            return;
        }

        String targetName = parts[1];
        if (targetName.startsWith("@")) {
            return;
        }

        if (isRealOnlinePlayerMatch(targetName)) {
            return;
        }

        Optional<FakePlayerRecord> targetRecord = fakePlayerService.findActiveByName(targetName);
        if (targetRecord.isEmpty()) {
            return;
        }

        String payload = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length)).trim();
        if (payload.isBlank()) {
            return;
        }

        event.setCancelled(true);
        Player sender = event.getPlayer();
        FakePlayerRecord fakeTarget = targetRecord.get();

        sender.sendMessage(ChatColor.GRAY + "You whisper to " + fakeTarget.getName() + ": " + payload);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTabComplete(TabCompleteEvent event) {
        String buffer = event.getBuffer();
        if (buffer == null || !buffer.startsWith("/")) {
            return;
        }

        String[] rawParts = buffer.substring(1).split("\\s+", -1);
        if (rawParts.length < 2) {
            return;
        }

        String command = normalizeCommand(rawParts[0]);
        if (!WHISPER_ALIASES.contains(command)) {
            return;
        }

        String targetPrefix = rawParts[1].trim().toLowerCase(Locale.ROOT);
        if (targetPrefix.startsWith("@")) {
            return;
        }

        List<String> completions = new ArrayList<>(event.getCompletions());
        Set<String> seen = new HashSet<>(completions);
        for (String fakeName : fakePlayerService.listActiveNames()) {
            if (!fakeName.toLowerCase(Locale.ROOT).startsWith(targetPrefix)) {
                continue;
            }
            if (seen.add(fakeName)) {
                completions.add(fakeName);
            }
        }
        event.setCompletions(completions);
    }

    private boolean isRealOnlinePlayerMatch(String targetName) {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(targetName)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeCommand(String rawCommand) {
        String lower = rawCommand.toLowerCase(Locale.ROOT);
        int namespaceSeparator = lower.lastIndexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator < lower.length() - 1) {
            return lower.substring(namespaceSeparator + 1);
        }
        return lower;
    }

}
