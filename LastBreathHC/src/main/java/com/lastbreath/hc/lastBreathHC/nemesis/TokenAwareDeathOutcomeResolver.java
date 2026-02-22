package com.lastbreath.hc.lastBreathHC.nemesis;

import com.lastbreath.hc.lastBreathHC.token.ReviveTokenHelper;
import org.bukkit.entity.Player;

public class TokenAwareDeathOutcomeResolver implements DeathOutcomeResolver {
    @Override
    public DeathOutcome resolve(Player player) {
        if (player == null || !player.isValid()) {
            return DeathOutcome.DEATH_PREVENTED;
        }
        return ReviveTokenHelper.hasToken(player)
                ? DeathOutcome.DEATH_SOFT
                : DeathOutcome.DEATH_FINAL;
    }
}
