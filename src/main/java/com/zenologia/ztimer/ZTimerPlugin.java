package com.zenologia.ztimer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.zenologia.ztimer.command.ZTimerCommand;
import com.zenologia.ztimer.config.ConfigManager;
import com.zenologia.ztimer.db.Storage;
import com.zenologia.ztimer.listener.PlayerJoinListener;
import com.zenologia.ztimer.listener.PlayerQuitListener;
import com.zenologia.ztimer.placeholder.ZTimerExpansion;
import com.zenologia.ztimer.timer.PendingTeleportManager;
import com.zenologia.ztimer.timer.TimerManager;

public class ZTimerPlugin extends JavaPlugin {

    private static ZTimerPlugin instance;

    private ConfigManager configManager;
    private Storage storage;
    private TimerManager timerManager;
    private PendingTeleportManager pendingTeleportManager;
    private ZTimerExpansion placeholderExpansion;

    public static ZTimerPlugin getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            getLogger().severe("PlaceholderAPI not found! ZTimer requires PlaceholderAPI. Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        try {
            this.storage = Storage.create(this, configManager);
            this.storage.initialize();
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize storage: " + ex.getMessage());
            ex.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        this.pendingTeleportManager = new PendingTeleportManager(this);
        this.timerManager = new TimerManager(this, storage, configManager, pendingTeleportManager);

        // Commands
        ZTimerCommand commandExecutor = new ZTimerCommand(this, timerManager, configManager);
        if (getCommand("ztimer") != null) {
            getCommand("ztimer").setExecutor(commandExecutor);
            getCommand("ztimer").setTabCompleter(commandExecutor);
        }

        // Listeners
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerQuitListener(timerManager, pendingTeleportManager), this);
        pm.registerEvents(new PlayerJoinListener(configManager, pendingTeleportManager), this);

        // PlaceholderAPI expansion
        this.placeholderExpansion = new ZTimerExpansion(this, timerManager, storage, configManager);
        this.placeholderExpansion.register();

        getLogger().info("ZTimer enabled.");
    }

    @Override
    public void onDisable() {
        if (placeholderExpansion != null) {
            placeholderExpansion.unregister();
        }
        if (storage != null) {
            storage.shutdown();
        }
        getLogger().info("ZTimer disabled.");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Storage getStorage() {
        return storage;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public PendingTeleportManager getPendingTeleportManager() {
        return pendingTeleportManager;
    }
}
