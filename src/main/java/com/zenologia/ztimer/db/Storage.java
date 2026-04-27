package com.zenologia.ztimer.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;

public abstract class Storage {

    protected final ZTimerPlugin plugin;
    protected final ConfigManager configManager;

    protected Storage(ZTimerPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public abstract void initialize() throws Exception;

    public abstract void shutdown();

    public abstract Long getBestTime(UUID playerUuid, String timerId) throws Exception;

    public abstract void updateBestTime(UUID playerUuid, String playerName, String timerId, long bestMillis, long nowMillis) throws Exception;

    public abstract void resetBestTime(UUID playerUuid, String timerId) throws Exception;

    public abstract void resetBestTimeForTimer(String timerId) throws Exception;

    public abstract List<LeaderboardEntry> getTopN(String timerId, int n) throws Exception;

    public abstract void updatePlayerName(UUID playerUuid, String playerName) throws Exception;

    protected void createSchema(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS ztimer_best_times (" +
                            "player_uuid  CHAR(36) NOT NULL," +
                            "player_name  VARCHAR(16) NOT NULL," +
                            "timer_id     VARCHAR(64) NOT NULL," +
                            "best_millis  BIGINT NOT NULL," +
                            "last_updated TIMESTAMP NOT NULL," +
                            "PRIMARY KEY (player_uuid, timer_id)" +
                            ")"
            );
            try {
                st.executeUpdate("CREATE INDEX idx_timer_best ON ztimer_best_times (timer_id, best_millis)");
            } catch (SQLException ignored) {
            }
        }
    }

    public static Storage create(ZTimerPlugin plugin, ConfigManager configManager) {
        String configuredType = configManager.getRawConfig().getString("storage.type");
        String type = configuredType == null ? "sqlite" : configuredType.toLowerCase(Locale.ROOT);

        switch (type) {
            case "mysql":
            case "mariadb":
                return new MysqlStorage(plugin, configManager);
            case "yaml":
                return new YamlStorage(plugin, configManager);
            case "sqlite":
            default:
                return new SqliteStorage(plugin, configManager);
        }
    }
}
