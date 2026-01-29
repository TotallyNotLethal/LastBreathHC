package com.lastbreath.hc.lastBreathHC.team;

public class TeamWaypoint {

    private final String label;
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;

    public TeamWaypoint(String label, String worldName, double x, double y, double z) {
        this.label = label;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public String getLabel() {
        return label;
    }

    public String getWorldName() {
        return worldName;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }
}
