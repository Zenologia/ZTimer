package com.zenologia.ztimer.listener;

import com.zenologia.ztimer.timer.PendingTeleportManager;
import com.zenologia.ztimer.timer.TimerManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final TimerManager timerManager;
    private final PendingTeleportManager pendingTeleportManager;

    public PlayerQuitListener(TimerManager timerManager, PendingTeleportManager pendingTeleportManager) {
        this.timerManager = timerManager;
        this.pendingTeleportManager = pendingTeleportManager;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        timerManager.handleLogout(player);
    }
}
