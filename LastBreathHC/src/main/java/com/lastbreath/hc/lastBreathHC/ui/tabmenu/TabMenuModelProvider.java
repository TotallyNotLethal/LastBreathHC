package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

public final class TabMenuModelProvider {
    private final TabMenuModelBuilder modelBuilder;
    private final TabMenuDataSource dataSource;

    public TabMenuModelProvider(LastBreathHC plugin) {
        this(plugin, Duration.ZERO);
    }

    public TabMenuModelProvider(LastBreathHC plugin, Duration refreshInterval) {
        Objects.requireNonNull(plugin, "plugin");
        TabMenuConfig config = TabMenuConfig.load(plugin);
        this.modelBuilder = new TabMenuModelBuilder(config);
        this.dataSource = new TabMenuDataSource(plugin, config.dateTime(), refreshInterval);
    }

    public TabMenuModel build(List<TabMenuModelBuilder.PlayerEntry> players) {
        TabMenuModelBuilder.TabMenuContext context = dataSource.getContext();
        return modelBuilder.build(context, players);
    }

    public void refresh() {
        dataSource.refresh();
    }
}
