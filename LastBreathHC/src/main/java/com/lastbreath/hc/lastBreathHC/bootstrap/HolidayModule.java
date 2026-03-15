package com.lastbreath.hc.lastBreathHC.bootstrap;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayEventConfig;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayEventManager;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayGameplayManager;
import com.lastbreath.hc.lastBreathHC.holiday.HolidayJoinListener;

public final class HolidayModule implements PluginModule {
    private final LastBreathHC plugin;
    private final ListenerRegistrar listenerRegistrar;
    private HolidayEventManager holidayEventManager;
    private HolidayGameplayManager holidayGameplayManager;

    public HolidayModule(LastBreathHC plugin, ListenerRegistrar listenerRegistrar) {
        this.plugin = plugin;
        this.listenerRegistrar = listenerRegistrar;
    }

    @Override
    public void register() {
        holidayEventManager = new HolidayEventManager();
        HolidayEventConfig holidayEventConfig = HolidayEventConfig.load(plugin);
        holidayGameplayManager = new HolidayGameplayManager(plugin, holidayEventManager, holidayEventConfig);

        listenerRegistrar.register(new HolidayJoinListener(holidayEventManager, holidayGameplayManager));
        listenerRegistrar.register(holidayGameplayManager);
        holidayGameplayManager.start();
    }

    @Override
    public void shutdown() {
        if (holidayGameplayManager != null) {
            holidayGameplayManager.stop();
        }
    }

    public HolidayEventManager holidayEventManager() {
        return holidayEventManager;
    }

    public HolidayGameplayManager holidayGameplayManager() {
        return holidayGameplayManager;
    }
}
