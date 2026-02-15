package com.lastbreath.hc.lastBreathHC.stats;

import com.lastbreath.hc.lastBreathHC.cosmetics.BossAura;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossKillMessage;
import com.lastbreath.hc.lastBreathHC.cosmetics.BossPrefix;
import com.lastbreath.hc.lastBreathHC.titles.Title;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerStats {
    public UUID uuid;
    public long timeAlive;
    public int deaths;
    public int revives;
    public int mobsKilled;
    public int asteroidLoots;
    public int cropsHarvested;
    public int blocksMined;
    public int blocksPlaced;
    public int fishCaught;
    public int playerKills;
    public int rareOresMined;
    public String nickname;
    public Set<Title> unlockedTitles = new HashSet<>();
    public Title equippedTitle;
    public boolean worldScalerEnabled;
    public Set<BossPrefix> unlockedPrefixes = new HashSet<>();
    public BossPrefix equippedPrefix;
    public Set<BossAura> unlockedAuras = new HashSet<>();
    public BossAura equippedAura;
    public Set<BossKillMessage> unlockedKillMessages = new HashSet<>();
    public BossKillMessage equippedKillMessage;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public PlayerStats() {
    }
}
