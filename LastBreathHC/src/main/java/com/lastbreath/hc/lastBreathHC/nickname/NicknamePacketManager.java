package com.lastbreath.hc.lastBreathHC.nickname;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import com.lastbreath.hc.lastBreathHC.titles.TitleManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Rewrites outgoing PlayerInfo packets so the profile name equals the resolved nickname.
 * This is the only reliable way to replace the in-world nametag username segment.
 */
public final class NicknamePacketManager implements Listener {

    private static NicknamePacketManager instance;

    private final Plugin plugin;
    private final ProtocolManager protocolManager;
    private PacketAdapter packetAdapter;

    private NicknamePacketManager(Plugin plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public static synchronized void initialize(Plugin plugin) {
        if (instance != null) {
            return;
        }
        if (plugin == null || Bukkit.getPluginManager().getPlugin("ProtocolLib") == null) {
            return;
        }

        NicknamePacketManager manager = new NicknamePacketManager(plugin);
        manager.registerPacketAdapter();
        Bukkit.getPluginManager().registerEvents(manager, plugin);
        instance = manager;

        for (Player online : Bukkit.getOnlinePlayers()) {
            refreshPlayerName(online);
        }
    }

    public static synchronized void shutdown() {
        if (instance == null) {
            return;
        }
        instance.unregisterPacketAdapter();
        HandlerListShim.unregisterAll(instance);
        instance = null;
    }

    public static void refreshPlayerName(Player player) {
        NicknamePacketManager active = instance;
        if (active == null || player == null || !player.isOnline()) {
            return;
        }
        active.refreshPlayerNameInternal(player);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshPlayerNameInternal(player), 1L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshPlayerNameInternal(player), 1L);
    }

    private void registerPacketAdapter() {
        List<PacketType> supportedTypes = resolveSupportedPlayerInfoTypes();
        if (supportedTypes.isEmpty()) {
            return;
        }

        packetAdapter = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                supportedTypes.toArray(new PacketType[0])) {
            @Override
            public void onPacketSending(PacketEvent event) {
                PacketContainer packet = event.getPacket();
                List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().readSafely(0);
                if (dataList == null || dataList.isEmpty()) {
                    return;
                }

                List<PlayerInfoData> rewritten = new ArrayList<>(dataList.size());
                boolean changed = false;
                for (PlayerInfoData data : dataList) {
                    PlayerInfoData updated = rewritePlayerInfoData(data);
                    rewritten.add(updated);
                    changed |= updated != data;
                }

                if (changed) {
                    packet.getPlayerInfoDataLists().write(0, rewritten);
                }
            }
        };

        protocolManager.addPacketListener(packetAdapter);
    }

    private void unregisterPacketAdapter() {
        if (packetAdapter != null) {
            protocolManager.removePacketListener(packetAdapter);
            packetAdapter = null;
        }
    }

    private List<PacketType> resolveSupportedPlayerInfoTypes() {
        List<PacketType> packetTypes = new ArrayList<>();
        addIfPresent(packetTypes, "PLAYER_INFO_UPDATE");
        addIfPresent(packetTypes, "PLAYER_INFO");
        return packetTypes;
    }

    private void addIfPresent(List<PacketType> packetTypes, String fieldName) {
        try {
            Object value = PacketType.Play.Server.class.getField(fieldName).get(null);
            if (value instanceof PacketType packetType) {
                packetTypes.add(packetType);
            }
        } catch (ReflectiveOperationException ignored) {
            // Packet does not exist on this server/protocollib combination.
        }
    }

    private PlayerInfoData rewritePlayerInfoData(PlayerInfoData original) {
        if (original == null) {
            return null;
        }

        WrappedGameProfile profile = original.getProfile();
        if (profile == null) {
            return original;
        }

        UUID uuid = profile.getUUID();
        if (uuid == null) {
            return original;
        }

        Player target = Bukkit.getPlayer(uuid);
        if (target == null) {
            return original;
        }

        String preferredName = sanitizeName(TitleManager.resolvePreferredProfileName(target), target.getName());
        if (preferredName.equals(profile.getName())) {
            return original;
        }

        WrappedGameProfile rewrittenProfile = copyProfileWithNewName(profile, preferredName);
        WrappedChatComponent rewrittenDisplayName = WrappedChatComponent.fromText(preferredName);
        return copyPlayerInfoData(original, rewrittenProfile, rewrittenDisplayName, uuid);
    }

    private WrappedGameProfile copyProfileWithNewName(WrappedGameProfile original, String newName) {
        WrappedGameProfile rewritten = new WrappedGameProfile(original.getUUID(), newName);

        Set<String> propertyNames = new LinkedHashSet<>();
        propertyNames.addAll(original.getProperties().keySet());
        for (String propertyName : propertyNames) {
            for (WrappedSignedProperty property : original.getProperties().get(propertyName)) {
                rewritten.getProperties().put(propertyName, property);
            }
        }
        return rewritten;
    }

    private PlayerInfoData copyPlayerInfoData(PlayerInfoData original,
                                              WrappedGameProfile profile,
                                              WrappedChatComponent displayName,
                                              UUID uuid) {
        try {
            for (Constructor<?> constructor : PlayerInfoData.class.getConstructors()) {
                Object[] args = mapConstructorArgs(constructor.getParameterTypes(), original, profile, displayName, uuid);
                if (args == null) {
                    continue;
                }
                return (PlayerInfoData) constructor.newInstance(args);
            }
        } catch (Exception ignored) {
            return original;
        }
        return original;
    }

    private Object[] mapConstructorArgs(Class<?>[] parameterTypes,
                                        PlayerInfoData original,
                                        WrappedGameProfile profile,
                                        WrappedChatComponent displayName,
                                        UUID uuid) {
        Object[] args = new Object[parameterTypes.length];
        int intIndex = 0;

        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> type = parameterTypes[i];
            if (WrappedGameProfile.class.isAssignableFrom(type)) {
                args[i] = profile;
                continue;
            }
            if (UUID.class.isAssignableFrom(type)) {
                args[i] = uuid;
                continue;
            }
            if (type == int.class || type == Integer.class) {
                args[i] = (intIndex++ == 0) ? original.getLatency() : invokeGetter(original, "getListOrder", Integer.class, 0);
                continue;
            }
            if (type == boolean.class || type == Boolean.class) {
                args[i] = invokeGetter(original, "isListed", Boolean.class, Boolean.TRUE);
                continue;
            }
            if (EnumWrappers.NativeGameMode.class.isAssignableFrom(type)) {
                args[i] = original.getGameMode();
                continue;
            }

            Object reflected = resolveOptionalValue(original, type, displayName);
            if (reflected == null && type.isPrimitive()) {
                return null;
            }
            args[i] = reflected;
        }
        return args;
    }

    private Object resolveOptionalValue(PlayerInfoData original, Class<?> targetType, WrappedChatComponent displayName) {
        if (targetType.getSimpleName().contains("WrappedChatComponent")) {
            return displayName;
        }
        Object listOrder = invokeGetterRaw(original, "getListOrder");
        if (listOrder != null && targetType.isInstance(listOrder)) {
            return listOrder;
        }
        Object listed = invokeGetterRaw(original, "isListed");
        if (listed != null && targetType.isInstance(listed)) {
            return listed;
        }
        Object profileKey = invokeGetterRaw(original, "getProfileKeyData");
        if (profileKey != null && targetType.isInstance(profileKey)) {
            return profileKey;
        }
        Object chatSession = invokeGetterRaw(original, "getRemoteChatSessionData");
        if (chatSession != null && targetType.isInstance(chatSession)) {
            return chatSession;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> T invokeGetter(PlayerInfoData original, String methodName, Class<T> targetType, T fallback) {
        Object value = invokeGetterRaw(original, methodName);
        if (value == null || !targetType.isInstance(value)) {
            return fallback;
        }
        return (T) value;
    }

    private Object invokeGetterRaw(PlayerInfoData original, String methodName) {
        try {
            Method method = original.getClass().getMethod(methodName);
            return method.invoke(original);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void refreshPlayerNameInternal(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }

        // Full remove/add viewer refresh prevents stale nametag caches and avoids duplicated names.
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) {
                continue;
            }
            viewer.hidePlayer(plugin, player);
            viewer.showPlayer(plugin, player);
        }

    }

    private String sanitizeName(String preferred, String fallback) {
        if (preferred == null || preferred.isBlank()) {
            return fallback;
        }
        String trimmed = preferred.trim();
        if (trimmed.length() > 16) {
            return trimmed.substring(0, 16);
        }
        return trimmed;
    }

    /**
     * Small shim so we don't need to pull extra Bukkit classes into static imports for shutdown.
     */
    private static final class HandlerListShim {
        private HandlerListShim() {
        }

        private static void unregisterAll(Listener listener) {
            org.bukkit.event.HandlerList.unregisterAll(listener);
        }
    }
}
