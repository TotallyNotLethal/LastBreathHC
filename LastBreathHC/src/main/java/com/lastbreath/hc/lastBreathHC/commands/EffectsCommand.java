package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.EffectsStatusGUI;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectManager;
import com.lastbreath.hc.lastBreathHC.potion.CustomPotionEffectRegistry;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class EffectsCommand implements BasicCommand {

    private static final int DEFAULT_DURATION_SECONDS = 120;

    private final CustomPotionEffectManager effectManager;
    private final CustomPotionEffectRegistry effectRegistry;
    private final EffectsStatusGUI effectsStatusGUI;

    public EffectsCommand(CustomPotionEffectManager effectManager,
                          CustomPotionEffectRegistry effectRegistry,
                          EffectsStatusGUI effectsStatusGUI) {
        this.effectManager = effectManager;
        this.effectRegistry = effectRegistry;
        this.effectsStatusGUI = effectsStatusGUI;
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        boolean isOp = source.getSender().isOp();
        List<String> rootOptions = isOp ? List.of("gui", "list", "give") : List.of("gui");
        if (args.length == 0) {
            return rootOptions;
        }
        if (args.length == 1) {
            return filterByPrefix(args[0], rootOptions);
        }

        if (!isOp) {
            return List.of();
        }

        if ("give".equalsIgnoreCase(args[0])) {
            if (args.length == 2) {
                return filterByPrefix(args[1], Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .sorted(Comparator.naturalOrder())
                        .toList());
            }
            if (args.length == 3) {
                return filterByPrefix(args[2], effectRegistry.getAll().stream()
                        .map(CustomPotionEffectRegistry.CustomPotionEffectDefinition::id)
                        .map(id -> id.toLowerCase(Locale.ROOT))
                        .sorted(Comparator.naturalOrder())
                        .toList());
            }
            if (args.length == 4) {
                return filterByPrefix(args[3], List.of("60", "120", "300"));
            }
        }

        return List.of();
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            Player target = resolveTarget(source, args.length > 1 ? args[1] : null);
            if (target == null) {
                source.getSender().sendMessage("§cPlayer not found.");
                return;
            }
            if (!source.getSender().isOp() && source.getSender() != target) {
                source.getSender().sendMessage("§cNo permission.");
                return;
            }
            effectsStatusGUI.open(target);
            return;
        }

        if (args[0].equalsIgnoreCase("list")) {
            if (!source.getSender().isOp()) {
                source.getSender().sendMessage("§cNo permission.");
                return;
            }
            List<String> ids = effectRegistry.getAll().stream()
                    .map(CustomPotionEffectRegistry.CustomPotionEffectDefinition::id)
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
            source.getSender().sendMessage("§6Custom effects: §f" + String.join("§7, §f", ids));
            return;
        }

        if (args[0].equalsIgnoreCase("give")) {
            if (!source.getSender().isOp()) {
                source.getSender().sendMessage("§cNo permission.");
                return;
            }
            if (args.length < 3) {
                source.getSender().sendMessage("§cUsage: /effects give <player> <effectId> [durationSeconds]");
                return;
            }
            Player target = resolveTarget(source, args[1]);
            if (target == null) {
                source.getSender().sendMessage("§cPlayer not found.");
                return;
            }
            String effectId = args[2].toLowerCase(Locale.ROOT);
            int durationSeconds = parseDurationSeconds(args.length > 3 ? args[3] : null);
            int durationTicks = durationSeconds * 20;
            if (!effectManager.activateEffect(target, effectId, durationTicks)) {
                source.getSender().sendMessage("§cUnknown effect ID: " + effectId);
                return;
            }
            source.getSender().sendMessage("§aApplied " + effectId + " to " + target.getName() + " for " + durationSeconds + "s.");
            return;
        }

        source.getSender().sendMessage("§cUsage: /effects [gui|list|give]");
    }

    private Player resolveTarget(CommandSourceStack source, String name) {
        if (name == null || name.isBlank()) {
            return source.getSender() instanceof Player player ? player : null;
        }
        return Bukkit.getPlayerExact(name);
    }

    private int parseDurationSeconds(String input) {
        if (input == null || input.isBlank()) {
            return DEFAULT_DURATION_SECONDS;
        }
        try {
            return Math.max(5, Integer.parseInt(input));
        } catch (NumberFormatException ex) {
            return DEFAULT_DURATION_SECONDS;
        }
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
