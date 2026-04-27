package com.zenologia.ztimer.db;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;

public class YamlStorage extends Storage {

    private File file;
    private YamlConfiguration yaml;

    public YamlStorage(ZTimerPlugin plugin, ConfigManager configManager) {
        super(plugin, configManager);
    }

    @Override
    public synchronized void initialize() throws Exception {
        String fileName = configManager.getRawConfig().getString("storage.yaml.file", "ztimer-data.yml");
        this.file = new File(plugin.getDataFolder(), fileName);

        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (!file.exists()) {
            yaml = new YamlConfiguration();
            yaml.createSection("players");
            yaml.save(file);
        }

        yaml = YamlConfiguration.loadConfiguration(file);
        if (!yaml.isConfigurationSection("players")) {
            yaml.set("players", null);
            yaml.createSection("players");
            save();
        }
    }

    @Override
    public void shutdown() {
    }

    @Override
    public synchronized Long getBestTime(UUID playerUuid, String timerId) {
        String path = timerPath(playerUuid, timerId) + ".best_millis";
        return yaml.contains(path) ? yaml.getLong(path) : null;
    }

    @Override
    public synchronized void updateBestTime(UUID playerUuid, String playerName, String timerId, long bestMillis, long nowMillis) throws Exception {
        ConfigurationSection playerSection = getOrCreatePlayerSection(playerUuid);
        playerSection.set("player_name", playerName);

        ConfigurationSection timersSection = playerSection.getConfigurationSection("timers");
        if (timersSection == null) {
            timersSection = playerSection.createSection("timers");
        }

        ConfigurationSection timerSection = timersSection.getConfigurationSection(timerId);
        if (timerSection == null) {
            timerSection = timersSection.createSection(timerId);
        }

        timerSection.set("best_millis", bestMillis);
        timerSection.set("last_updated", nowMillis);
        save();
    }

    @Override
    public synchronized void resetBestTime(UUID playerUuid, String timerId) throws Exception {
        ConfigurationSection timersSection = yaml.getConfigurationSection("players." + playerUuid + ".timers");
        if (timersSection == null) {
            return;
        }

        timersSection.set(timerId, null);
        cleanupPlayerSection(playerUuid.toString());
        save();
    }

    @Override
    public synchronized void resetBestTimeForTimer(String timerId) throws Exception {
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            return;
        }

        List<String> playerIds = new ArrayList<>(playersSection.getKeys(false));
        for (String playerId : playerIds) {
            ConfigurationSection timersSection = playersSection.getConfigurationSection(playerId + ".timers");
            if (timersSection == null) {
                continue;
            }

            timersSection.set(timerId, null);
            cleanupPlayerSection(playerId);
        }

        save();
    }

    @Override
    public synchronized List<LeaderboardEntry> getTopN(String timerId, int n) {
        List<LeaderboardEntry> entries = new ArrayList<>();
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            return entries;
        }

        for (String playerId : playersSection.getKeys(false)) {
            String timerPath = "players." + playerId + ".timers." + timerId;
            if (!yaml.contains(timerPath + ".best_millis")) {
                continue;
            }

            try {
                UUID uuid = UUID.fromString(playerId);
                String playerName = yaml.getString("players." + playerId + ".player_name", playerId);
                long bestMillis = yaml.getLong(timerPath + ".best_millis");
                entries.add(new LeaderboardEntry(uuid, playerName, bestMillis));
            } catch (IllegalArgumentException ignored) {
            }
        }

        entries.sort(Comparator.comparingLong(LeaderboardEntry::getBestMillis));
        if (entries.size() <= n) {
            return entries;
        }
        return new ArrayList<>(entries.subList(0, n));
    }

    @Override
    public synchronized void updatePlayerName(UUID playerUuid, String playerName) throws Exception {
        ConfigurationSection playerSection = yaml.getConfigurationSection("players." + playerUuid);
        if (playerSection == null) {
            return;
        }

        playerSection.set("player_name", playerName);
        save();
    }

    private ConfigurationSection getOrCreatePlayerSection(UUID playerUuid) {
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection == null) {
            playersSection = yaml.createSection("players");
        }

        ConfigurationSection playerSection = playersSection.getConfigurationSection(playerUuid.toString());
        if (playerSection == null) {
            playerSection = playersSection.createSection(playerUuid.toString());
        }

        return playerSection;
    }

    private void cleanupPlayerSection(String playerId) {
        ConfigurationSection playerSection = yaml.getConfigurationSection("players." + playerId);
        if (playerSection == null) {
            return;
        }

        ConfigurationSection timersSection = playerSection.getConfigurationSection("timers");
        if (timersSection != null && !timersSection.getKeys(false).isEmpty()) {
            return;
        }

        yaml.set("players." + playerId, null);
    }

    private String timerPath(UUID playerUuid, String timerId) {
        return "players." + playerUuid + ".timers." + timerId;
    }

    private void save() throws IOException {
        yaml.save(file);
    }
}
