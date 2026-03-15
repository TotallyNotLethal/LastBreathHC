package com.lastbreath.hc.lastBreathHC.bootstrap;

public interface PluginModule {
    void register();

    default void shutdown() {
        // optional
    }
}
