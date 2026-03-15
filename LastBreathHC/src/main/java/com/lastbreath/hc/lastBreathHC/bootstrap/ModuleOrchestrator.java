package com.lastbreath.hc.lastBreathHC.bootstrap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleOrchestrator {
    private final List<PluginModule> modules;

    public ModuleOrchestrator(List<PluginModule> modules) {
        this.modules = List.copyOf(modules);
    }

    public void registerAll() {
        for (PluginModule module : modules) {
            module.register();
        }
    }

    public void shutdownAll() {
        List<PluginModule> shutdownOrder = new ArrayList<>(modules);
        Collections.reverse(shutdownOrder);
        for (PluginModule module : shutdownOrder) {
            module.shutdown();
        }
    }
}
