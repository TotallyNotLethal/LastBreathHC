package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.DailyRewardGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.List;

public class DailyCommand implements BasicCommand {

    private final DailyRewardGUI dailyRewardGUI;

    public DailyCommand(DailyRewardGUI dailyRewardGUI) {
        this.dailyRewardGUI = dailyRewardGUI;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        dailyRewardGUI.open(player);
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }
}
