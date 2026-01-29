package com.lastbreath.hc.lastBreathHC.ui.tabmenu;

import com.lastbreath.hc.lastBreathHC.LastBreathHC;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer.TabMenuRenderResult;
import com.lastbreath.hc.lastBreathHC.ui.tabmenu.renderer.TabMenuRenderer;
import java.util.EnumSet;
import java.util.Objects;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class TabMenuRefreshScheduler {
    private final LastBreathHC plugin;
    private final TabMenuModelProvider modelProvider;
    private final TabMenuRenderer renderer;
    private final TabMenuPlayerSource playerSource;
    private final TabMenuUpdateHandler updateHandler;
    private final long refreshTicks;
    private BukkitTask task;
    private TabMenuModel lastModel;
    private final EnumSet<TabMenuSection> pendingSections = EnumSet.noneOf(TabMenuSection.class);

    public TabMenuRefreshScheduler(LastBreathHC plugin,
                                   TabMenuModelProvider modelProvider,
                                   TabMenuRenderer renderer,
                                   TabMenuPlayerSource playerSource,
                                   TabMenuUpdateHandler updateHandler,
                                   long refreshTicks) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.modelProvider = Objects.requireNonNull(modelProvider, "modelProvider");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        this.playerSource = Objects.requireNonNull(playerSource, "playerSource");
        this.updateHandler = Objects.requireNonNull(updateHandler, "updateHandler");
        this.refreshTicks = Math.max(1L, refreshTicks);
    }

    public void start() {
        if (task != null) {
            task.cancel();
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                refresh();
            }
        }.runTaskTimer(plugin, 0L, refreshTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void requestRefresh(TabMenuSection... sections) {
        if (sections == null || sections.length == 0) {
            return;
        }
        synchronized (pendingSections) {
            for (TabMenuSection section : sections) {
                if (section != null) {
                    pendingSections.add(section);
                }
            }
        }
    }

    private void refresh() {
        TabMenuModel model = modelProvider.build(playerSource.getPlayers());
        EnumSet<TabMenuSection> changedSections = diffSections(model);
        if (changedSections.isEmpty()) {
            return;
        }
        TabMenuRenderResult renderResult = renderer.render(model);
        updateHandler.apply(new TabMenuUpdate(changedSections, model, renderResult));
        lastModel = model;
    }

    private EnumSet<TabMenuSection> diffSections(TabMenuModel model) {
        EnumSet<TabMenuSection> changed = drainPendingSections();
        if (lastModel == null) {
            changed.addAll(EnumSet.allOf(TabMenuSection.class));
            return changed;
        }
        if (!Objects.equals(lastModel.header(), model.header())) {
            changed.add(TabMenuSection.HEADER);
        }
        if (!Objects.equals(lastModel.footer(), model.footer())) {
            changed.add(TabMenuSection.FOOTER);
        }
        if (!Objects.equals(lastModel.players(), model.players())) {
            changed.add(TabMenuSection.PLAYERS);
        }
        return changed;
    }

    private EnumSet<TabMenuSection> drainPendingSections() {
        synchronized (pendingSections) {
            if (pendingSections.isEmpty()) {
                return EnumSet.noneOf(TabMenuSection.class);
            }
            EnumSet<TabMenuSection> requested = EnumSet.copyOf(pendingSections);
            pendingSections.clear();
            return requested;
        }
    }
}
