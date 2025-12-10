package com.zenologia.ztimer.db;

import java.util.UUID;

public class LeaderboardEntry {

    private final UUID playerUuid;
    private final String playerName;
    private final long bestMillis;

    public LeaderboardEntry(UUID playerUuid, String playerName, long bestMillis) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.bestMillis = bestMillis;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public long getBestMillis() {
        return bestMillis;
    }
}
