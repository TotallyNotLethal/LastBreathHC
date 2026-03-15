package com.lastbreath.hc.lastBreathHC.bootstrap;

import org.bukkit.event.Listener;

@FunctionalInterface
public interface ListenerRegistrar {
    void register(Listener listener);
}
