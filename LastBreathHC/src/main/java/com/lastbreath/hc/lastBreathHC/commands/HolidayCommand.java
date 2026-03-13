package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.holiday.HolidayEventManager;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayWeekEvent;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayGameplayManager;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class HolidayCommand implements BasicCommand {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM d");
    private final HolidayEventManager holidayEventManager;
    private final HolidayGameplayManager holidayGameplayManager;

    public HolidayCommand(HolidayEventManager holidayEventManager, HolidayGameplayManager holidayGameplayManager) {
        this.holidayEventManager = holidayEventManager;
        this.holidayGameplayManager = holidayGameplayManager;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        var sender = source.getSender();

        if (args.length > 0 && args[0].equalsIgnoreCase("progress") && sender instanceof Player player) {
            sender.sendMessage(Component.text(holidayGameplayManager.progressLine(player), NamedTextColor.YELLOW));
            return;
        }

        holidayEventManager.getActiveHolidayEvent().ifPresentOrElse(active -> {
            sender.sendMessage(Component.text("Holiday event is LIVE: ", NamedTextColor.GOLD)
                    .append(Component.text(active.holidayType().displayName(), NamedTextColor.AQUA)));
            sender.sendMessage(Component.text("Event: " + holidayGameplayManager.getActiveDefinition().map(d -> d.eventName()).orElse(active.holidayType().eventName()), NamedTextColor.GREEN));
            sender.sendMessage(Component.text("Objective: " + holidayGameplayManager.getActiveDefinition().map(d -> d.objective()).orElse(active.holidayType().objective()), NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Window: " + DATE_FORMAT.format(active.weekStart()) + " - " + DATE_FORMAT.format(active.weekEnd()), NamedTextColor.GRAY));
        }, () -> {
            sender.sendMessage(Component.text("No holiday event active right now.", NamedTextColor.RED));
            List<HolidayWeekEvent> upcoming = holidayEventManager.upcomingEvents(3);
            sender.sendMessage(Component.text("Upcoming holiday weeks:", NamedTextColor.GOLD));
            for (HolidayWeekEvent event : upcoming) {
                sender.sendMessage(Component.text("- " + event.holidayType().displayName() + " (" + DATE_FORMAT.format(event.weekStart()) + " - " + DATE_FORMAT.format(event.weekEnd()) + ")", NamedTextColor.GRAY));
            }
        });
    }
}
