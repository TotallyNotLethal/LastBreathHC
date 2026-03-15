package com.lastbreath.hc.lastBreathHC.holiday;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class HolidayJoinListener implements Listener {

    private final HolidayEventManager holidayEventManager;
    private final HolidayGameplayManager holidayGameplayManager;

    public HolidayJoinListener(HolidayEventManager holidayEventManager, HolidayGameplayManager holidayGameplayManager) {
        this.holidayEventManager = holidayEventManager;
        this.holidayGameplayManager = holidayGameplayManager;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        holidayEventManager.getActiveHolidayEvent().ifPresent(active -> {
            String eventName = holidayGameplayManager.getActiveDefinition()
                    .map(HolidayEventDefinition::eventName)
                    .orElse(active.holidayType().eventName());
            String objective = holidayGameplayManager.getActiveDefinition()
                    .map(HolidayEventDefinition::objective)
                    .orElse(active.holidayType().objective());

            event.getPlayer().sendMessage(Component.text("[Holiday] ", NamedTextColor.GOLD)
                    .append(Component.text(active.holidayType().displayName() + " week is active! ", NamedTextColor.AQUA))
                    .append(Component.text(eventName, NamedTextColor.GREEN)));
            event.getPlayer().sendMessage(Component.text(objective, NamedTextColor.YELLOW));
        });
    }
}
