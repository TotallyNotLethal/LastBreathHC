package com.lastbreath.hc.lastBreathHC.fakeplayer.platform.v1_21_11;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Paper 1.21.11 adapter using version-specific internal packet classes via reflection.
 *
 * <p>This adapter intentionally lives in a versioned package so all internal class references are
 * isolated from generic fake-player logic.</p>
 */
public class Paper12111FakePlayerAdapter implements FakePlayerPlatformAdapter {

    private static final String ACTION_ADD_PLAYER = "ADD_PLAYER";

    private final LastBreathHC plugin;
    private final Map<UUID, Object> fakeHandles = new ConcurrentHashMap<>();
    private boolean failed;

    public Paper12111FakePlayerAdapter(LastBreathHC plugin) {
        this.plugin = plugin;
    }

    @Override
    public void spawnFakeTabEntry(FakePlayerRecord record) {
        if (record == null || !record.isActive() || hasFailed()) {
            return;
        }
        try {
            Object serverPlayer = fakeHandles.computeIfAbsent(record.getUuid(), key -> createServerPlayer(record));
            if (serverPlayer == null) {
                return;
            }
            Object addPacket = createPlayerInfoUpdatePacket(ACTION_ADD_PLAYER, List.of(serverPlayer));
            broadcastPacket(addPacket);
        } catch (Throwable throwable) {
            markFailed("Unable to spawn fake tab entry", throwable);
        }
    }

    @Override
    public void despawnFakeTabEntry(UUID uuid) {
        if (uuid == null || hasFailed()) {
            return;
        }
        try {
            fakeHandles.remove(uuid);
            Object removePacket = createPlayerInfoRemovePacket(List.of(uuid));
            broadcastPacket(removePacket);
        } catch (Throwable throwable) {
            markFailed("Unable to despawn fake tab entry", throwable);
        }
    }

    @Override
    public void updateDisplayState(FakePlayerRecord record) {
        if (record == null || hasFailed()) {
            return;
        }
        despawnFakeTabEntry(record.getUuid());
        if (record.isActive()) {
            spawnFakeTabEntry(record);
        }
    }

    private Object createServerPlayer(FakePlayerRecord record) {
        try {
            Class<?> craftServerClass = Class.forName("org.bukkit.craftbukkit.CraftServer");
            Class<?> craftWorldClass = Class.forName("org.bukkit.craftbukkit.CraftWorld");
            Class<?> minecraftServerClass = Class.forName("net.minecraft.server.MinecraftServer");
            Class<?> serverLevelClass = Class.forName("net.minecraft.server.level.ServerLevel");
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> clientInformationClass = Class.forName("net.minecraft.server.level.ClientInformation");
            Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");

            Method getServerMethod = craftServerClass.getMethod("getServer");
            Object minecraftServer = getServerMethod.invoke(Bukkit.getServer());
            if (!minecraftServerClass.isInstance(minecraftServer)) {
                return null;
            }

            World world = Bukkit.getWorlds().stream().findFirst().orElse(null);
            if (world == null || !craftWorldClass.isInstance(world)) {
                return null;
            }
            Method getHandleMethod = craftWorldClass.getMethod("getHandle");
            Object serverLevel = getHandleMethod.invoke(world);
            if (!serverLevelClass.isInstance(serverLevel)) {
                return null;
            }

            Object profile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(record.getUuid(), record.getName());

            applySkin(profile, record);

            Method createDefaultMethod = clientInformationClass.getMethod("createDefault");
            Object clientInfo = createDefaultMethod.invoke(null);

            return serverPlayerClass
                    .getConstructor(minecraftServerClass, serverLevelClass, gameProfileClass, clientInformationClass)
                    .newInstance(minecraftServer, serverLevel, profile, clientInfo);
        } catch (Throwable throwable) {
            markFailed("Unable to create fake server player handle", throwable);
            return null;
        }
    }

    private void applySkin(Object profile, FakePlayerRecord record) throws Exception {
        if (record.getTextures() == null || record.getTextures().isBlank()) {
            return;
        }

        Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
        Object propertyMap = getPropertiesMethod.invoke(profile);

        Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
        Object property = record.getSignature() == null || record.getSignature().isBlank()
                ? propertyClass.getConstructor(String.class, String.class)
                .newInstance("textures", record.getTextures())
                : propertyClass.getConstructor(String.class, String.class, String.class)
                .newInstance("textures", record.getTextures(), record.getSignature());

        Method putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
        putMethod.invoke(propertyMap, "textures", property);
    }

    private Object createPlayerInfoUpdatePacket(String actionName, Collection<?> players) throws Exception {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        @SuppressWarnings({"rawtypes", "unchecked"})
        EnumSet<?> actionSet = EnumSet.of(Enum.valueOf((Class<? extends Enum>) actionClass, actionName));
        return packetClass.getConstructor(EnumSet.class, Collection.class).newInstance(actionSet, players);
    }

    private Object createPlayerInfoRemovePacket(List<UUID> uuids) throws Exception {
        Class<?> removePacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket");
        return removePacketClass.getConstructor(List.class).newInstance(uuids);
    }

    private void broadcastPacket(Object packet) throws Exception {
        if (packet == null) {
            return;
        }

        Class<?> craftPlayerClass = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
        Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");

        Method getHandleMethod = craftPlayerClass.getMethod("getHandle");
        Method sendMethod = null;

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!craftPlayerClass.isInstance(player)) {
                continue;
            }
            Object handle = getHandleMethod.invoke(player);
            if (!serverPlayerClass.isInstance(handle)) {
                continue;
            }
            Object connection = serverPlayerClass.getField("connection").get(handle);
            if (connection == null) {
                continue;
            }
            if (sendMethod == null) {
                Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");
                sendMethod = connection.getClass().getMethod("send", packetInterface);
            }
            sendMethod.invoke(connection, packet);
        }
    }

    private boolean hasFailed() {
        return failed;
    }

    private void markFailed(String message, Throwable throwable) {
        failed = true;
        plugin.getLogger().log(Level.WARNING, message + ". Visual simulation will be disabled until restart.", throwable);
    }
}
