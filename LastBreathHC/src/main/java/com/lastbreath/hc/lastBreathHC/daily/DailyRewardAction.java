package com.lastbreath.hc.lastBreathHC.daily;

import org.bukkit.entity.Player;

public interface DailyRewardAction {
    String preview();

    String grant(Player player);
}
