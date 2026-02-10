package com.lastbreath.hc.lastBreathHC.fakeplayer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Locale;
import java.util.Optional;

/**
 * Intercepts /kill for fake players so they follow the fake-player death pipeline.
 */
public class FakePlayerKillCommandListener implements Listener {

    private final FakePlayerService fakePlayerService;

    public FakePlayerKillCommandListener(FakePlayerService fakePlayerService) {
        this.fakePlayerService = fakePlayerService;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onKillCommand(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage();
        if (message == null || message.isBlank() || !message.startsWith("/")) {
            return;
        }

        String[] parts = message.substring(1).trim().split("\\s+");
        if (parts.length < 2) {
            return;
        }

        String command = normalizeCommand(parts[0]);
        if (!"kill".equals(command)) {
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

        event.setCancelled(true);

        FakePlayerRecord fakeTarget = targetRecord.get();
        boolean killed = fakePlayerService.killFakePlayer(fakeTarget.getUuid(), event.getPlayer().getName());
        if (killed) {
            event.getPlayer().sendMessage("§aKilled fake player: §f" + fakeTarget.getName());
            return;
        }

        event.getPlayer().sendMessage("§cFailed to kill fake player: " + fakeTarget.getName());
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
