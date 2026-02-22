package com.lastbreath.hc.lastBreathHC.heads;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;

import java.util.*;

public class HeadManager {

    // UUID -> Stored Ender Chest
    private static final Map<UUID, Inventory> HEAD_LOOT = new HashMap<>();
    private static final Map<UUID, Inventory> PENDING_RESTORE = new HashMap<>();

    private static NamespacedKey KEY;
    private static NamespacedKey RECORD_KEY;

    public static void init() {
        KEY = new NamespacedKey(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("LastBreathHC")),
                "death_head"
        );
        RECORD_KEY = new NamespacedKey(
                Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("LastBreathHC")),
                "death_head_record"
        );
    }

    /* ============================
       STORAGE
       ============================ */

    public static void store(UUID uuid, Inventory inv) {
        HEAD_LOOT.put(uuid, inv);
    }

    public static Inventory get(UUID uuid) {
        return HEAD_LOOT.get(uuid);
    }

    public static boolean has(UUID uuid) {
        return HEAD_LOOT.containsKey(uuid);
    }

    public static void remove(UUID uuid) {
        HEAD_LOOT.remove(uuid);
    }

    /* ============================
       PENDING RESTORE
       ============================ */

    public static void storePendingRestore(UUID uuid, Inventory inv) {
        PENDING_RESTORE.put(uuid, inv);
    }

    public static Inventory getPendingRestore(UUID uuid) {
        return PENDING_RESTORE.get(uuid);
    }

    public static boolean hasPendingRestore(UUID uuid) {
        return PENDING_RESTORE.containsKey(uuid);
    }

    public static void removePendingRestore(UUID uuid) {
        PENDING_RESTORE.remove(uuid);
    }

    /* ============================
       ACCESS
       ============================ */

    public static Map<UUID, Inventory> getAll() {
        return Collections.unmodifiableMap(HEAD_LOOT);
    }

    public static NamespacedKey getKey() {
        return KEY;
    }

    public static NamespacedKey getRecordKey() {
        return RECORD_KEY;
    }
}
