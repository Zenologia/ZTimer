package com.zenologia.ztimer.timer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PendingTeleportManager {

    private final Map<UUID, String> pendingTeleportByPlayer = new ConcurrentHashMap<>();

    public void setPendingTeleport(UUID playerId, String timerId) {
        if (timerId == null) {
            pendingTeleportByPlayer.remove(playerId);
        } else {
            pendingTeleportByPlayer.put(playerId, timerId);
        }
    }

    public String consumePendingTeleport(UUID playerId) {
        return pendingTeleportByPlayer.remove(playerId);
    }

    public boolean hasPendingTeleport(UUID playerId) {
        return pendingTeleportByPlayer.containsKey(playerId);
    }
}
