package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.titles.Title;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import com.lastbreath.hc.lastBreathHC.gui.TitlesGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TitlesCommand implements BasicCommand {

    private final TitlesGUI titlesGUI;

    public TitlesCommand(TitlesGUI titlesGUI) {
        this.titlesGUI = titlesGUI;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            return List.of();
        }

        if (args.length == 0) {
            return List.of("list", "equip", "background");
        }

        if (args.length == 1) {
            return filterByPrefix(args[0], List.of("list", "equip", "background"));
        }

        if ("equip".equalsIgnoreCase(args[0])) {
            String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            return TitleManager.getUnlockedTitleInputs(player).stream()
                    .filter(title -> title.toLowerCase(Locale.ROOT).startsWith(input.toLowerCase(Locale.ROOT)))
                    .toList();
        }

        if ("background".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return filterByPrefix(args[1], List.of("on", "off", "toggle", "status"));
            }
            return List.of();
        }

        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            titlesGUI.open(player);
            return;
        }

        if (args[0].equalsIgnoreCase("equip")) {
            if (args.length < 2) {
                player.sendMessage("§cUsage: /titles equip <title>");
                return;
            }
            String input = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            Title title = Title.fromInput(input);
            if (title == null) {
                player.sendMessage("§cUnknown title. Use /titles list to see options.");
                return;
            }
            if (TitleManager.isBackgroundOnlyTitle(title)) {
                player.sendMessage("§e" + title.displayName() + " is a background title. Use /titles background <on|off|toggle|status>.");
                return;
            }
            if (!TitleManager.equipTitle(player, title)) {
                player.sendMessage("§cYou have not unlocked that title yet.");
                return;
            }
            player.sendMessage("§aEquipped title: " + title.displayName());
            return;
        }

        if (args[0].equalsIgnoreCase("background")) {
            if (args.length < 2 || args[1].equalsIgnoreCase("status")) {
                boolean enabled = TitleManager.isWorldScalerEnabled(player);
                player.sendMessage("§7World Scaler background title: " + (enabled ? "§aEnabled" : "§cDisabled"));
                return;
            }
            boolean enable;
            if (args[1].equalsIgnoreCase("on")) {
                enable = true;
            } else if (args[1].equalsIgnoreCase("off")) {
                enable = false;
            } else if (args[1].equalsIgnoreCase("toggle")) {
                enable = !TitleManager.isWorldScalerEnabled(player);
            } else {
                player.sendMessage("§cUsage: /titles background <on|off|toggle|status>");
                return;
            }
            if (!TitleManager.setWorldScalerEnabled(player, enable)) {
                player.sendMessage("§cYou have not unlocked the World Scaler background title yet.");
                return;
            }
            player.sendMessage("§aWorld Scaler background title " + (enable ? "enabled" : "disabled") + ".");
            return;
        }

        player.sendMessage("§cUsage: /titles [list|equip <title>|background <on|off|toggle|status>]");
    }

    private List<String> filterByPrefix(String input, List<String> options) {
        if (input == null || input.isBlank()) {
            return options;
        }
        String lowered = input.toLowerCase(Locale.ROOT);
        return options.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(lowered))
                .toList();
    }
}
