package com.zenologia.ztimer.db;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqliteStorage extends Storage {

    private Connection connection;

    public SqliteStorage(ZTimerPlugin plugin, ConfigManager configManager) {
        super(plugin, configManager);
    }

    @Override
    public void initialize() throws Exception {
        String fileName = configManager.getRawConfig().getString("storage.sqlite.file", "ztimer.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);
        if (!dbFile.getParentFile().exists()) {
            //noinspection ResultOfMethodCallIgnored
            dbFile.getParentFile().mkdirs();
        }
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        createSchema(connection);
    }

    @Override
    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ignored) {
            }
        }
    }

    @Override
    public Long getBestTime(UUID playerUuid, String timerId) throws Exception {
        String sql = "SELECT best_millis FROM ztimer_best_times WHERE player_uuid = ? AND timer_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, timerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("best_millis");
                }
                return null;
            }
        }
    }

    @Override
    public void updateBestTime(UUID playerUuid, String playerName, String timerId, long bestMillis, long nowMillis) throws Exception {
        String sql = "INSERT INTO ztimer_best_times (player_uuid, player_name, timer_id, best_millis, last_updated) " +
                "VALUES (?, ?, ?, ?, ?) " +
                "ON CONFLICT(player_uuid, timer_id) DO UPDATE SET " +
                "player_name = excluded.player_name, best_millis = excluded.best_millis, last_updated = excluded.last_updated";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, playerName);
            ps.setString(3, timerId);
            ps.setLong(4, bestMillis);
            ps.setTimestamp(5, new Timestamp(nowMillis));
            ps.executeUpdate();
        }
    }

    @Override
    public void resetBestTime(UUID playerUuid, String timerId) throws Exception {
        String sql = "DELETE FROM ztimer_best_times WHERE player_uuid = ? AND timer_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, timerId);
            ps.executeUpdate();
        }
    }


    @Override
    public void updatePlayerName(java.util.UUID playerUuid, String playerName) throws Exception {
        String sql = "UPDATE ztimer_best_times SET player_name = ? WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public List<LeaderboardEntry> getTopN(String timerId, int n) throws Exception {
        String sql = "SELECT player_uuid, player_name, best_millis FROM ztimer_best_times " +
                "WHERE timer_id = ? ORDER BY best_millis ASC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, timerId);
            ps.setInt(2, n);
            List<LeaderboardEntry> result = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    UUID uuid = UUID.fromString(rs.getString("player_uuid"));
                    String name = rs.getString("player_name");
                    long best = rs.getLong("best_millis");
                    result.add(new LeaderboardEntry(uuid, name, best));
                }
            }
            return result;
        }
    }


    @Override
    public void resetBestTimeForTimer(String timerId) throws Exception {
        String sql = "DELETE FROM ztimer_best_times WHERE timer_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, timerId);
            ps.executeUpdate();
        }
    }

}
