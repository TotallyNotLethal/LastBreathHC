package com.lastbreath.hc.lastBreathHC.holiday;

import org.bukkit.Location;

public record HolidayEventZone(String world, double x, double y, double z, double radius) {

    public boolean contains(Location location) {
        if (location == null || location.getWorld() == null) {
            return false;
        }
        if (world != null && !world.isBlank() && !world.equalsIgnoreCase(location.getWorld().getName())) {
            return false;
        }
        double dx = location.getX() - x;
        double dy = location.getY() - y;
        double dz = location.getZ() - z;
        return (dx * dx + dy * dy + dz * dz) <= radius * radius;
    }
}
