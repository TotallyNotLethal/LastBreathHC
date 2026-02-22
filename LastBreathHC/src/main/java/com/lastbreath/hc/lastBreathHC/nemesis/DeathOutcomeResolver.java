package com.lastbreath.hc.lastBreathHC.nemesis;

import org.bukkit.entity.Player;

public interface DeathOutcomeResolver {

    DeathOutcome resolve(Player player);

    enum DeathOutcome {
        DEATH_FINAL,
        DEATH_PREVENTED,
        DEATH_SOFT
    }
}
