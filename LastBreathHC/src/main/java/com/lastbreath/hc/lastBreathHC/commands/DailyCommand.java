package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.DailyRewardGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.function.Supplier;

public class DailyCommand implements BasicCommand {

    private final Supplier<DailyRewardGUI> dailyRewardGUISupplier;

    public DailyCommand(DailyRewardGUI dailyRewardGUI) {
        this(() -> dailyRewardGUI);
    }

    public DailyCommand(Supplier<DailyRewardGUI> dailyRewardGUISupplier) {
        this.dailyRewardGUISupplier = dailyRewardGUISupplier;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }

        DailyRewardGUI dailyRewardGUI = dailyRewardGUISupplier.get();
        if (dailyRewardGUI == null) {
            player.sendMessage("Daily rewards are still loading, please try again in a moment.");
            return;
        }

        dailyRewardGUI.open(player);
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }
}
