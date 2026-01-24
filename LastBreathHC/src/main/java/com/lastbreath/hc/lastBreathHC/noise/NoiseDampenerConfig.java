package com.lastbreath.hc.lastBreathHC.noise;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NoiseDampenerConfig {

    private final Location location;
    private final Set<String> dampenedSoundKeys = new HashSet<>();
    private final List<String> recentSoundKeys = new ArrayList<>();

    public NoiseDampenerConfig(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public Set<String> getDampenedSoundKeys() {
        return dampenedSoundKeys;
    }

    public List<String> getRecentSoundKeys() {
        return recentSoundKeys;
    }
}
