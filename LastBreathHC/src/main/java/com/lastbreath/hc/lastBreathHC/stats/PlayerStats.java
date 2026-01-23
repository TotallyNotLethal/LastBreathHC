package com.lastbreath.hc.lastBreathHC.stats;

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
    public int rareOresMined;
    public Set<Title> unlockedTitles = new HashSet<>();
    public Title equippedTitle;

    public PlayerStats(UUID uuid) {
        this.uuid = uuid;
    }

    public PlayerStats() {
    }
}
