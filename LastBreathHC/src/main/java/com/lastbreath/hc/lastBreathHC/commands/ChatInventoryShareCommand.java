package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.chat.ChatInventoryShareService;
import com.lastbreath.hc.lastBreathHC.chat.ChatInventoryShareService.ShareType;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;

public class ChatInventoryShareCommand implements BasicCommand {

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player viewer)) {
            return;
        }
        if (args.length < 2) {
            return;
        }
        ShareType type = ChatInventoryShareService.parseType(args[0]);
        if (type == null) {
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(args[1]);
        } catch (IllegalArgumentException ex) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            viewer.sendMessage(Component.text("That player is not online.", NamedTextColor.RED));
            return;
        }

        ChatInventoryShareService.openShare(viewer, target, type);
    }
}
