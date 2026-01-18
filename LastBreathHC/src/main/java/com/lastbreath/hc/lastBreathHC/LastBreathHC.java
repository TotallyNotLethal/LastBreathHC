package com.lastbreath.hc.lastBreathHC;

import com.lastbreath.hc.lastBreathHC.asteroid.AsteroidListener;
import com.lastbreath.hc.lastBreathHC.heads.HeadListener;
import com.lastbreath.hc.lastBreathHC.heads.HeadManager;
import com.lastbreath.hc.lastBreathHC.titles.TitleListener;
import org.bukkit.plugin.java.JavaPlugin;
import com.lastbreath.hc.lastBreathHC.token.TokenRecipe;
import com.lastbreath.hc.lastBreathHC.gui.ReviveGUI;
import com.lastbreath.hc.lastBreathHC.death.DeathListener;

public final class LastBreathHC extends JavaPlugin {

    private static LastBreathHC instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        getLogger().info("LastBreathHC enabled.");
        HeadManager.init();

        getServer().getPluginManager().registerEvents(
                new DeathListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new HeadListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new ReviveGUI(), this
        );
        getServer().getPluginManager().registerEvents(
                new AsteroidListener(), this
        );
        getServer().getPluginManager().registerEvents(
                new TitleListener(), this
        );

        TokenRecipe.register();

        getLifecycleManager().registerEventHandler(
                io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents.COMMANDS,
                event -> event.registrar().register(
                        "asteroid",
                        new com.lastbreath.hc.lastBreathHC.commands.AsteroidCommand()
                ).register(
                        "titles",
                        new com.lastbreath.hc.lastBreathHC.commands.TitlesCommand()
                )
        );
    }

    @Override
    public void onDisable() {
        getLogger().info("LastBreathHC disabled.");
    }

    public static LastBreathHC getInstance() {
        return instance;
    }
}
