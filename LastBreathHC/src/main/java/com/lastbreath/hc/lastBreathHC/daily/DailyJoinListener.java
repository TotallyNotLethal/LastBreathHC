package com.lastbreath.hc.lastBreathHC.daily;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.gui.DailyRewardGUI;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class DailyJoinListener implements Listener {

    private final LastBreathHC plugin;
    private final DailyRewardManager dailyRewardManager;
    private final DailyRewardGUI dailyRewardGUI;

    public DailyJoinListener(LastBreathHC plugin, DailyRewardManager dailyRewardManager, DailyRewardGUI dailyRewardGUI) {
        this.plugin = plugin;
        this.dailyRewardManager = dailyRewardManager;
        this.dailyRewardGUI = dailyRewardGUI;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        boolean firstJoinToday = dailyRewardManager.markJoin(player.getUniqueId());
        boolean canClaim = dailyRewardManager.canClaimToday(player.getUniqueId());

        if (canClaim && dailyRewardManager.isNotifyOnJoin()) {
            player.sendMessage(ChatColor.GOLD + "Reminder: " + ChatColor.YELLOW + "check /daily - your reward is ready to claim.");
        }

        if (canClaim && firstJoinToday && dailyRewardManager.isAutoOpenOnFirstJoinOfDay()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> dailyRewardGUI.open(player));
        }
    }
}
