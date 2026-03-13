package com.lastbreath.hc.lastBreathHC.holiday;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class HolidayJoinListener implements Listener {

    private final HolidayEventManager holidayEventManager;

    public HolidayJoinListener(HolidayEventManager holidayEventManager) {
        this.holidayEventManager = holidayEventManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        holidayEventManager.getActiveHolidayEvent().ifPresent(active -> {
            event.getPlayer().sendMessage(Component.text("[Holiday] ", NamedTextColor.GOLD)
                    .append(Component.text(active.holidayType().displayName() + " week is active! ", NamedTextColor.AQUA))
                    .append(Component.text(active.holidayType().eventName(), NamedTextColor.GREEN)));
            event.getPlayer().sendMessage(Component.text(active.holidayType().objective(), NamedTextColor.YELLOW));
        });
    }
}
