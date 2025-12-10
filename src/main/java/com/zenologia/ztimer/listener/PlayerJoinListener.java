package com.zenologia.ztimer.listener;

import com.zenologia.ztimer.config.ConfigManager;
import com.zenologia.ztimer.timer.PendingTeleportManager;
import com.zenologia.ztimer.timer.TimerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinListener implements Listener {

    private final ConfigManager configManager;
    private final PendingTeleportManager pendingTeleportManager;

    public PlayerJoinListener(ConfigManager configManager, PendingTeleportManager pendingTeleportManager) {
        this.configManager = configManager;
        this.pendingTeleportManager = pendingTeleportManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Delegate teleport handling to TimerManager via plugin instance
        Bukkit.getScheduler().runTaskLater(com.zenologia.ztimer.ZTimerPlugin.getInstance(), () -> {
            com.zenologia.ztimer.ZTimerPlugin.getInstance().getTimerManager().handleJoin(player);
        }, 1L);
    }
}
