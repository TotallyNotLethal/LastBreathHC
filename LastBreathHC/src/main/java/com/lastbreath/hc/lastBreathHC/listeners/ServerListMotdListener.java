package com.lastbreath.hc.lastBreathHC.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class ServerListMotdListener implements Listener {
    private static final List<String> BRACKET_STYLES = List.of(
            "[%s]",
            "[%s | Bloodbound]",
            "[%s | Ironclad]",
            "[%s | No Mercy]",
            "[%s | Redline]",
            "[%s | Deathmarch]",
            "[%s | Ashen Oath]"
    );

    private static final List<String> WITTY_LINES = List.of(
            "Bleed slow, fight loud, legends don't ever logout.",
            "Steel bites harder when hearts have no respawns.",
            "Grind the night, break the dawn, keep marching.",
            "No soft landings, only glory and hard lessons.",
            "Die brave, revive never, leave a lasting mark.",
            "Scars are currency; spend them on hard-won victory.",
            "Courage hits harder when the clock runs out."
    );

    @EventHandler
    public void onServerListPing(ServerListPingEvent event) {
        String serverName = resolveServerName();
        String bracketed = String.format(randomEntry(BRACKET_STYLES), serverName);
        String topLine = ChatColor.GOLD + bracketed + ChatColor.RED + " Hardcore";
        String bottomLine = ChatColor.GRAY + randomEntry(WITTY_LINES);
        event.setMotd(topLine + "\n" + bottomLine);
    }

    private String resolveServerName() {
        Server server = Bukkit.getServer();
        String name = "Last Breath";
        if (name == null || name.isBlank() || name.equalsIgnoreCase("Unknown Server")) {
            name = server.getName();
        }
        if (name == null || name.isBlank()) {
            name = "LastBreathHC";
        }
        return name.trim();
    }

    private String randomEntry(List<String> entries) {
        return entries.get(ThreadLocalRandom.current().nextInt(entries.size()));
    }
}
