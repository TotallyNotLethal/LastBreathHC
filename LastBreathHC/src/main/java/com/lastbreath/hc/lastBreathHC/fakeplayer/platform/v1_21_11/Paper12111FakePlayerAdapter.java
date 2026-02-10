package com.lastbreath.hc.lastBreathHC.fakeplayer.platform.v1_21_11;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.fakeplayer.FakePlayerRecord;
import com.lastbreath.hc.lastBreathHC.fakeplayer.platform.FakePlayerPlatformAdapter;
import com.lastbreath.hc.lastBreathHC.titles.Title;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            Object entryPacket = createPlayerInfoEntryPacket(record);
            if (entryPacket != null) {
                broadcastPacket(entryPacket);
                return;
            }

            Object serverPlayer = fakeHandles.computeIfAbsent(record.getUuid(), key -> createServerPlayer(record));
            if (serverPlayer == null) {
                return;
            }
            try {
                applyTabListMetadata(serverPlayer, record);
                applyListed(serverPlayer, true);
            } catch (Exception ignored) {
            }
            Object addPacket = createPlayerInfoUpdatePacket(ACTION_ADD_PLAYER, List.of(serverPlayer));
            broadcastPacket(addPacket);
            try {
                sendTabListUpdates(serverPlayer);
            } catch (Exception ignored) {
            }
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

    @Override
    public Optional<Player> getBukkitPlayer(FakePlayerRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        try {
            Object serverPlayer = fakeHandles.computeIfAbsent(record.getUuid(), key -> createServerPlayer(record));
            if (serverPlayer == null) {
                return Optional.empty();
            }
            Method getBukkitEntityMethod = serverPlayer.getClass().getMethod("getBukkitEntity");
            Object bukkitEntity = getBukkitEntityMethod.invoke(serverPlayer);
            if (bukkitEntity instanceof Player player) {
                return Optional.of(player);
            }
        } catch (Throwable ignored) {
            // Ignore resolution failures to avoid disabling fake-player visuals.
        }
        return Optional.empty();
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

    private void applyTabListMetadata(Object serverPlayer, FakePlayerRecord record) {
        if (serverPlayer == null || record == null) {
            return;
        }
        Title title = resolveTitle(record.getTabTitleKey());
        String legacyListName = "§7[" + title.tabTag() + "§7] " + record.getName();
        Player bukkitPlayer = getBukkitPlayer(record).orElse(null);
        if (bukkitPlayer != null) {
            bukkitPlayer.setPlayerListName(legacyListName);
        }
        Object nmsComponent = createNmsComponent(legacyListName);
        if (nmsComponent != null) {
            applyTabListName(serverPlayer, nmsComponent);
        }
        applyLatency(serverPlayer, Math.max(0, record.getTabPingMillis()));
    }

    private void sendTabListUpdates(Object serverPlayer) throws Exception {
        Object listedPacket = createPlayerInfoUpdatePacket("UPDATE_LISTED", List.of(serverPlayer));
        broadcastPacket(listedPacket);
        Object displayPacket = createPlayerInfoUpdatePacket("UPDATE_DISPLAY_NAME", List.of(serverPlayer));
        broadcastPacket(displayPacket);
        Object latencyPacket = createPlayerInfoUpdatePacket("UPDATE_LATENCY", List.of(serverPlayer));
        broadcastPacket(latencyPacket);
    }

    private void applyListed(Object serverPlayer, boolean listed) {
        try {
            Method method = serverPlayer.getClass().getMethod("setListed", boolean.class);
            method.invoke(serverPlayer, listed);
            return;
        } catch (Exception ignored) {
        }
        try {
            Field field = serverPlayer.getClass().getDeclaredField("listed");
            field.setAccessible(true);
            field.setBoolean(serverPlayer, listed);
        } catch (Exception ignored) {
        }
    }

    private void applyLatency(Object serverPlayer, int pingMillis) {
        try {
            Field field = serverPlayer.getClass().getDeclaredField("latency");
            field.setAccessible(true);
            field.setInt(serverPlayer, pingMillis);
            return;
        } catch (Exception ignored) {
        }
        try {
            Method method = serverPlayer.getClass().getMethod("setLatency", int.class);
            method.invoke(serverPlayer, pingMillis);
        } catch (Exception ignored) {
        }
    }

    private void applyTabListName(Object serverPlayer, Object component) {
        try {
            Method method = serverPlayer.getClass().getMethod("setTabListDisplayName", component.getClass());
            method.invoke(serverPlayer, component);
            return;
        } catch (Exception ignored) {
        }
        try {
            Method method = serverPlayer.getClass().getMethod("setTabListName", component.getClass());
            method.invoke(serverPlayer, component);
            return;
        } catch (Exception ignored) {
        }
        try {
            Field field = serverPlayer.getClass().getDeclaredField("listName");
            field.setAccessible(true);
            field.set(serverPlayer, component);
        } catch (Exception ignored) {
        }
    }

    private Object createNmsComponent(String legacyText) {
        try {
            Object adventureComponent = LegacyComponentSerializer.legacySection().deserialize(legacyText);
            Class<?> paperAdventure = Class.forName("io.papermc.paper.adventure.PaperAdventure");
            Method asVanilla = paperAdventure.getMethod("asVanilla", Class.forName("net.kyori.adventure.text.Component"));
            return asVanilla.invoke(null, adventureComponent);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Title resolveTitle(String tabTitleKey) {
        Title title = Title.fromInput(tabTitleKey);
        return title == null ? Title.WANDERER : title;
    }

    private void applySkin(Object profile, FakePlayerRecord record) {
        if (record.getTextures() == null || record.getTextures().isBlank()) {
            return;
        }

        try {
            Object propertyMap = resolvePropertyMap(profile);
            if (propertyMap == null) {
                return;
            }

            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            Object property = record.getSignature() == null || record.getSignature().isBlank()
                    ? propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", record.getTextures())
                    : propertyClass.getConstructor(String.class, String.class, String.class)
                    .newInstance("textures", record.getTextures(), record.getSignature());

            Method putMethod;
            try {
                putMethod = propertyMap.getClass().getMethod("put", Object.class, Object.class);
            } catch (NoSuchMethodException ignored) {
                putMethod = propertyMap.getClass().getMethod("put", String.class, propertyClass);
            }
            putMethod.invoke(propertyMap, "textures", property);
        } catch (Throwable ignored) {
            // Skin application is best-effort; keep fake player functional even if reflection fails.
        }
    }

    private Object resolvePropertyMap(Object profile) throws Exception {
        try {
            Method getPropertiesMethod = profile.getClass().getMethod("getProperties");
            return getPropertiesMethod.invoke(profile);
        } catch (NoSuchMethodException ignored) {
            // Fall through to alternate accessors.
        }
        try {
            Method propertiesMethod = profile.getClass().getMethod("properties");
            return propertiesMethod.invoke(profile);
        } catch (NoSuchMethodException ignored) {
            // Fall through to fields.
        }
        try {
            java.lang.reflect.Field propertiesField = profile.getClass().getDeclaredField("properties");
            propertiesField.setAccessible(true);
            return propertiesField.get(profile);
        } catch (NoSuchFieldException ignored) {
            // Fall through to alternate field names.
        }
        java.lang.reflect.Field propertyMapField = profile.getClass().getDeclaredField("propertyMap");
        propertyMapField.setAccessible(true);
        return propertyMapField.get(profile);
    }

    private Object createPlayerInfoUpdatePacket(String actionName, Collection<?> players) throws Exception {
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
        Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
        @SuppressWarnings({"rawtypes", "unchecked"})
        EnumSet<?> actionSet = EnumSet.of(Enum.valueOf((Class<? extends Enum>) actionClass, actionName));
        return packetClass.getConstructor(EnumSet.class, Collection.class).newInstance(actionSet, players);
    }

    private Object createPlayerInfoEntryPacket(FakePlayerRecord record) {
        try {
            Class<?> packetClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket");
            Class<?> actionClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Action");
            Class<?> entryClass = Class.forName("net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry");
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> gameTypeClass = Class.forName("net.minecraft.world.level.GameType");

            Object profile = gameProfileClass.getConstructor(UUID.class, String.class)
                    .newInstance(record.getUuid(), record.getName());
            applySkin(profile, record);

            Object gameType = Enum.valueOf((Class<? extends Enum>) gameTypeClass, "SURVIVAL");
            Object displayName = createNmsComponent("§7[" + resolveTitle(record.getTabTitleKey()).tabTag() + "§7] " + record.getName());

            Object entry = constructPlayerInfoEntry(entryClass, record, profile, gameType, displayName);
            if (entry == null) {
                return null;
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            EnumSet<?> actions = EnumSet.of(
                    Enum.valueOf((Class<? extends Enum>) actionClass, "ADD_PLAYER"),
                    Enum.valueOf((Class<? extends Enum>) actionClass, "UPDATE_LISTED"),
                    Enum.valueOf((Class<? extends Enum>) actionClass, "UPDATE_LATENCY"),
                    Enum.valueOf((Class<? extends Enum>) actionClass, "UPDATE_GAME_MODE"),
                    Enum.valueOf((Class<? extends Enum>) actionClass, "UPDATE_DISPLAY_NAME")
            );

            Constructor<?> ctor = Arrays.stream(packetClass.getConstructors())
                    .filter(c -> c.getParameterCount() == 2)
                    .filter(c -> EnumSet.class.isAssignableFrom(c.getParameterTypes()[0]))
                    .filter(c -> Collection.class.isAssignableFrom(c.getParameterTypes()[1]))
                    .findFirst()
                    .orElse(null);
            if (ctor == null) {
                return null;
            }
            return ctor.newInstance(actions, List.of(entry));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private Object constructPlayerInfoEntry(Class<?> entryClass,
                                            FakePlayerRecord record,
                                            Object gameProfile,
                                            Object gameType,
                                            Object displayName) {
        for (Constructor<?> constructor : entryClass.getConstructors()) {
            Object[] args = new Object[constructor.getParameterCount()];
            Class<?>[] params = constructor.getParameterTypes();
            boolean compatible = true;
            for (int i = 0; i < params.length; i++) {
                Class<?> type = params[i];
                if (type == UUID.class) {
                    args[i] = record.getUuid();
                } else if (type.getName().equals("com.mojang.authlib.GameProfile")) {
                    args[i] = gameProfile;
                } else if (type == int.class || type == Integer.class) {
                    args[i] = Math.max(0, record.getTabPingMillis());
                } else if (type == boolean.class || type == Boolean.class) {
                    args[i] = true;
                } else if (type.getName().equals("net.minecraft.world.level.GameType")) {
                    args[i] = gameType;
                } else if (type.getName().equals("net.minecraft.network.chat.Component")) {
                    args[i] = displayName;
                } else if (type.isPrimitive()) {
                    compatible = false;
                    break;
                } else {
                    args[i] = null;
                }
            }
            if (!compatible) {
                continue;
            }
            try {
                return constructor.newInstance(args);
            } catch (Exception ignored) {
            }
        }
        return null;
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
        Class<?> packetInterface = Class.forName("net.minecraft.network.protocol.Packet");

        Method getHandleMethod = craftPlayerClass.getMethod("getHandle");

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!craftPlayerClass.isInstance(player)) {
                continue;
            }
            Object handle = getHandleMethod.invoke(player);
            if (!serverPlayerClass.isInstance(handle)) {
                continue;
            }
            Object connection = resolveConnection(handle, serverPlayerClass);
            if (connection == null) {
                continue;
            }
            sendPacket(connection, packet, packetInterface);
        }
    }

    private Object resolveConnection(Object serverPlayer, Class<?> serverPlayerClass) {
        try {
            return serverPlayerClass.getField("connection").get(serverPlayer);
        } catch (Exception ignored) {
        }
        try {
            Field declared = serverPlayerClass.getDeclaredField("connection");
            declared.setAccessible(true);
            return declared.get(serverPlayer);
        } catch (Exception ignored) {
        }
        try {
            Method getter = serverPlayerClass.getMethod("connection");
            return getter.invoke(serverPlayer);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void sendPacket(Object connection, Object packet, Class<?> packetInterface) throws Exception {
        Method sendMethod = null;
        try {
            sendMethod = connection.getClass().getMethod("send", packetInterface);
        } catch (NoSuchMethodException ignored) {
            // Fall through to overloaded variants.
        }
        if (sendMethod != null) {
            sendMethod.invoke(connection, packet);
            return;
        }

        for (Method method : connection.getClass().getMethods()) {
            if (!method.getName().equals("send") || method.getParameterCount() == 0) {
                continue;
            }
            Class<?> first = method.getParameterTypes()[0];
            if (!first.isAssignableFrom(packet.getClass()) && !first.isAssignableFrom(packetInterface)) {
                continue;
            }
            Object[] args = new Object[method.getParameterCount()];
            args[0] = packet;
            method.invoke(connection, args);
            return;
        }

        throw new NoSuchMethodException("Unable to locate connection.send(Packet) for "
                + connection.getClass().getName());
    }

    private boolean hasFailed() {
        return failed;
    }

    private void markFailed(String message, Throwable throwable) {
        failed = true;
        plugin.getLogger().log(Level.WARNING, message + ". Visual simulation will be disabled until restart.", throwable);
    }
}
