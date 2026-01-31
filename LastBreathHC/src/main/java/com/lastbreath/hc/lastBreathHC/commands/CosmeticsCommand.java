package com.lastbreath.hc.lastBreathHC.commands;

import com.lastbreath.hc.lastBreathHC.gui.CosmeticsGUI;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.List;
import org.bukkit.entity.Player;

public class CosmeticsCommand implements BasicCommand {

    private final CosmeticsGUI cosmeticsGUI;

    public CosmeticsCommand(CosmeticsGUI cosmeticsGUI) {
        this.cosmeticsGUI = cosmeticsGUI;
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        if (!(source.getSender() instanceof Player player)) {
            source.getSender().sendMessage("Players only.");
            return;
        }
        cosmeticsGUI.open(player);
    }

    @Override
    public List<String> suggest(CommandSourceStack source, String[] args) {
        return List.of();
    }
}
