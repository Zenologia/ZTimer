package com.zenologia.ztimer.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MysqlStorage extends Storage {

    private HikariDataSource dataSource;

    public MysqlStorage(ZTimerPlugin plugin, ConfigManager configManager) {
        super(plugin, configManager);
    }

    @Override
    public void initialize() throws Exception {
        String host = configManager.getRawConfig().getString("storage.mysql.host", "localhost");
        int port = configManager.getRawConfig().getInt("storage.mysql.port", 3306);
        String database = configManager.getRawConfig().getString("storage.mysql.database", "ztimer");
        String user = configManager.getRawConfig().getString("storage.mysql.user", "ztimer");
        String password = configManager.getRawConfig().getString("storage.mysql.password", "password");
        boolean useSSL = configManager.getRawConfig().getBoolean("storage.mysql.useSSL", false);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&serverTimezone=UTC");
        hikariConfig.setUsername(user);
        hikariConfig.setPassword(password);
        hikariConfig.setMaximumPoolSize(10);
        hikariConfig.setPoolName("ZTimer-Hikari");

        this.dataSource = new HikariDataSource(hikariConfig);

        try (Connection connection = dataSource.getConnection()) {
            createSchema(connection);
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public Long getBestTime(UUID playerUuid, String timerId) throws Exception {
        String sql = "SELECT best_millis FROM ztimer_best_times WHERE player_uuid = ? AND timer_id = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
                "ON DUPLICATE KEY UPDATE " +
                "player_name = VALUES(player_name), best_millis = VALUES(best_millis), last_updated = VALUES(last_updated)";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, timerId);
            ps.executeUpdate();
        }
    }


    @Override
    public void updatePlayerName(java.util.UUID playerUuid, String playerName) throws Exception {
        String sql = "UPDATE ztimer_best_times SET player_name = ? WHERE player_uuid = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerName);
            ps.setString(2, playerUuid.toString());
            ps.executeUpdate();
        }
    }

    @Override
    public List<LeaderboardEntry> getTopN(String timerId, int n) throws Exception {
        String sql = "SELECT player_uuid, player_name, best_millis FROM ztimer_best_times " +
                "WHERE timer_id = ? ORDER BY best_millis ASC LIMIT ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
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
        try (java.sql.Connection connection = dataSource.getConnection();
             java.sql.PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, timerId);
            ps.executeUpdate();
        }
    }

}
