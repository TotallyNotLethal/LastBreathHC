package com.lastbreath.hc.lastBreathHC.mobs;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.spawners.SpawnerTags;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MobStackSignListener implements Listener {

    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final int SEARCH_RADIUS = 10;
    private static final int MIN_RADIUS = 1;
    private static final int MAX_RADIUS = 16;

    private final NamespacedKey playerPlacedSpawnerKey;
    private final NamespacedKey stackEnabledKey;
    private final NamespacedKey aiEnabledKey;
    private final NamespacedKey stackRadiusKey;
    private final NamespacedKey aiRadiusKey;
    private final Map<SignKey, SignState> signCache = new HashMap<>();

    /**
     * AI signs use the same toggle flow as stack signs: shift-right-click toggles between enabled
     * (green, AI enabled) and disabled (red, AI disabled). Newly placed AI signs default to disabled.
     */
    public MobStackSignListener(LastBreathHC plugin) {
        this.playerPlacedSpawnerKey = new NamespacedKey(plugin, SpawnerTags.PLAYER_PLACED_SPAWNER_KEY);
        this.stackEnabledKey = new NamespacedKey(plugin, "stack_enabled");
        this.aiEnabledKey = new NamespacedKey(plugin, "ai_enabled");
        this.stackRadiusKey = new NamespacedKey(plugin, "stack_radius");
        this.aiRadiusKey = new NamespacedKey(plugin, "ai_radius");
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        SignType signType = parseSignType(event);
        if (signType == null || !hasOnlyFirstLine(event)) {
            return;
        }

        RadiusData radiusData = parseRadius(event.line(3));
        if (!hasNearbyPlayerPlacedSpawner(event.getBlock())) {
            return;
        }

        boolean enabled = signType == SignType.STACK;
        if (signType == SignType.AI) {
            enabled = false;
        }

        setSignLines(event, signType, enabled, radiusData);
        updateSignState(event.getBlock(), signType, enabled, radiusData);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking() || event.getClickedBlock() == null) {
            return;
        }

        if (!event.getAction().isRightClick()) {
            return;
        }

        Block block = event.getClickedBlock();
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        SignState state = getSignState(sign);
        if (state == null) {
            return;
        }

        boolean newEnabled = !state.enabled();
        updateSignState(block, state.type(), newEnabled, null);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        SignState state = getSignState(sign);
        if (state == null) {
            return;
        }

        signCache.remove(SignKey.from(block.getLocation()));
    }

    private SignType parseSignType(SignChangeEvent event) {
        String line = PLAIN_TEXT.serialize(event.line(0)).trim();
        if (line.equalsIgnoreCase("stack")) {
            return SignType.STACK;
        }
        if (line.equalsIgnoreCase("ai")) {
            return SignType.AI;
        }
        return null;
    }

    private boolean hasOnlyFirstLine(SignChangeEvent event) {
        for (int i = 1; i < 3; i++) {
            String line = PLAIN_TEXT.serialize(event.line(i));
            if (!line.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasNearbyPlayerPlacedSpawner(Block signBlock) {
        Location origin = signBlock.getLocation();
        if (origin.getWorld() == null) {
            return false;
        }

        int originX = origin.getBlockX();
        int originY = origin.getBlockY();
        int originZ = origin.getBlockZ();
        int radiusSquared = SEARCH_RADIUS * SEARCH_RADIUS;

        for (int x = originX - SEARCH_RADIUS; x <= originX + SEARCH_RADIUS; x++) {
            for (int y = originY - SEARCH_RADIUS; y <= originY + SEARCH_RADIUS; y++) {
                for (int z = originZ - SEARCH_RADIUS; z <= originZ + SEARCH_RADIUS; z++) {
                    Block block = origin.getWorld().getBlockAt(x, y, z);
                    if (block.getType() != Material.SPAWNER) {
                        continue;
                    }
                    if (origin.distanceSquared(block.getLocation()) > radiusSquared) {
                        continue;
                    }
                    if (!(block.getState() instanceof CreatureSpawner spawner)) {
                        continue;
                    }
                    if (spawner.getPersistentDataContainer().has(playerPlacedSpawnerKey, PersistentDataType.BYTE)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void setSignLines(SignChangeEvent event, SignType signType, boolean enabled, RadiusData radiusData) {
        Component label = Component.text(signType.label(), enabled ? NamedTextColor.GREEN : NamedTextColor.RED);
        event.line(0, label);
        for (int i = 1; i < 3; i++) {
            event.line(i, Component.empty());
        }
        if (radiusData != null && radiusData.shouldUpdateLine()) {
            event.line(3, Component.text(String.valueOf(radiusData.radius())));
        }
    }

    private SignState getSignState(Sign sign) {
        PersistentDataContainer container = sign.getPersistentDataContainer();
        if (container.has(stackEnabledKey, PersistentDataType.BYTE)) {
            return new SignState(SignType.STACK, isEnabled(container, stackEnabledKey));
        }
        if (container.has(aiEnabledKey, PersistentDataType.BYTE)) {
            return new SignState(SignType.AI, isEnabled(container, aiEnabledKey));
        }
        SignKey key = SignKey.from(sign.getLocation());
        return signCache.get(key);
    }

    private void updateSignState(Block block, SignType type, boolean enabled, RadiusData radiusOverride) {
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        PersistentDataContainer container = sign.getPersistentDataContainer();
        RadiusData radiusData = radiusOverride == null ? parseRadius(sign.line(3)) : radiusOverride;
        int radius = clampRadius(radiusData.radius());
        if (type == SignType.STACK) {
            container.set(stackEnabledKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
            container.remove(aiEnabledKey);
            container.set(stackRadiusKey, PersistentDataType.INTEGER, radius);
            container.remove(aiRadiusKey);
        } else {
            container.set(aiEnabledKey, PersistentDataType.BYTE, enabled ? (byte) 1 : (byte) 0);
            container.remove(stackEnabledKey);
            container.set(aiRadiusKey, PersistentDataType.INTEGER, radius);
            container.remove(stackRadiusKey);
        }

        Component label = Component.text(type.label(), enabled ? NamedTextColor.GREEN : NamedTextColor.RED);
        sign.line(0, label);
        for (int i = 1; i < 3; i++) {
            sign.line(i, Component.empty());
        }
        if (radiusData.shouldUpdateLine()) {
            sign.line(3, Component.text(String.valueOf(radius)));
        }
        sign.update();

        signCache.put(SignKey.from(block.getLocation()), new SignState(type, enabled));
    }

    private boolean isEnabled(PersistentDataContainer container, NamespacedKey key) {
        Byte value = container.get(key, PersistentDataType.BYTE);
        return value != null && value > 0;
    }

    private RadiusData parseRadius(Component line) {
        String raw = PLAIN_TEXT.serialize(line).trim();
        if (raw.isEmpty()) {
            return new RadiusData(SEARCH_RADIUS, false);
        }
        try {
            int parsed = Integer.parseInt(raw);
            int clamped = clampRadius(parsed);
            return new RadiusData(clamped, clamped != parsed);
        } catch (NumberFormatException ex) {
            return new RadiusData(SEARCH_RADIUS, false);
        }
    }

    private int clampRadius(int radius) {
        if (radius < MIN_RADIUS) {
            return MIN_RADIUS;
        }
        if (radius > MAX_RADIUS) {
            return MAX_RADIUS;
        }
        return radius;
    }

    private enum SignType {
        STACK("Stack"),
        AI("AI");

        private final String label;

        SignType(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    private record SignState(SignType type, boolean enabled) {
    }

    private record RadiusData(int radius, boolean shouldUpdateLine) {
    }

    private record SignKey(UUID worldId, int x, int y, int z) {
        public static SignKey from(Location location) {
            return new SignKey(
                    location.getWorld() == null ? new UUID(0L, 0L) : location.getWorld().getUID(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ()
            );
        }
    }
}
