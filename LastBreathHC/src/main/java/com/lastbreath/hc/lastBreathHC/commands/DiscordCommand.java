package com.lastbreath.hc.lastBreathHC.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public class DiscordCommand implements BasicCommand {

    private static final String INVITE_URL = "https://discord.gg/tUmddDxc";

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        source.getSender().sendMessage(
                Component.text("Join our Discord: ", NamedTextColor.AQUA)
                        .append(Component.text(INVITE_URL, NamedTextColor.BLUE)
                                .clickEvent(ClickEvent.openUrl(INVITE_URL))
                                .underlined(true))
        );
    }
}
