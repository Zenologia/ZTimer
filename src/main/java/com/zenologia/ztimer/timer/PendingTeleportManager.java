package com.zenologia.ztimer.timer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import com.zenologia.ztimer.ZTimerPlugin;

/**
 * Persistence-backed pending-teleport manager.
 * Stores pending teleports to plugin data folder/pending_teleports.yml using Bukkit YamlConfiguration.
 */
public class PendingTeleportManager {

    private final Map<UUID, PendingTeleport> cache = new ConcurrentHashMap<>();
    private final File file;
    private YamlConfiguration yaml;
    private final ZTimerPlugin plugin;

    public PendingTeleportManager(ZTimerPlugin plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        this.file = new File(dataFolder, "pending_teleports.yml");
        loadFromFile();
    }

    private synchronized void loadFromFile() {
        try {
            if (!file.exists()) {
                yaml = new YamlConfiguration();
                yaml.set("players", new HashMap<>());
                yaml.save(file);
            }
            yaml = YamlConfiguration.loadConfiguration(file);
            cache.clear();

            if (yaml.contains("players")) {
                ConfigurationSectionWrapper sec = new ConfigurationSectionWrapper(yaml, "players");
                for (String key : sec.getKeys()) {
                    try {
                        UUID uuid = UUID.fromString(key);
                        String timerId = sec.getString(key + ".timer_id", null);
                        List<String> commands = sec.getStringList(key + ".commands");
                        if (timerId != null) {
                            cache.put(uuid, new PendingTeleport(timerId, commands));
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load pending teleports file: " + ex.getMessage(), ex);
            yaml = new YamlConfiguration();
        }
    }

    private synchronized void saveToFileAsync() {
        final YamlConfiguration copy = new YamlConfiguration();
        Map<String, Object> players = new HashMap<>();
        for (Map.Entry<UUID, PendingTeleport> e : cache.entrySet()) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("timer_id", e.getValue().getTimerId());
            entry.put("commands", e.getValue().getCommands());
            players.put(e.getKey().toString(), entry);
        }
        copy.set("players", players);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                copy.save(file);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save pending teleports file: " + ex.getMessage(), ex);
            }
        });
    }

    /**
     * Sets (or clears if timerId==null) a pending teleport and persists it.
     * commands may be null or empty.
     */
    public void setPendingTeleport(UUID playerId, String timerId, List<String> commands) {
        if (timerId == null) {
            cache.remove(playerId);
        } else {
            PendingTeleport pt = new PendingTeleport(timerId, commands == null ? Collections.emptyList() : commands.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList()));
            cache.put(playerId, pt);
        }
        saveToFileAsync();
    }

    /**
     * Backwards-compatible single-arg setter used by existing code paths.
     */
    public void setPendingTeleport(UUID playerId, String timerId) {
        setPendingTeleport(playerId, timerId, null);
    }

    /**
     * Consume pending teleport (removes from cache and persisted store). Returns null if none.
     */
    public PendingTeleport consumePendingTeleport(UUID playerId) {
        PendingTeleport pt = cache.remove(playerId);
        if (pt != null) {
            saveToFileAsync();
        }
        return pt;
    }

    public boolean hasPendingTeleport(UUID playerId) {
        return cache.containsKey(playerId);
    }

    // Minimal wrapper to avoid direct use of YAML ConfigurationSection in iteration above
    private static class ConfigurationSectionWrapper {
        private final YamlConfiguration yaml;
        private final String base;

        ConfigurationSectionWrapper(YamlConfiguration yaml, String base) {
            this.yaml = yaml;
            this.base = base;
        }

        Set<String> getKeys() {
            if (!yaml.contains(base)) return Collections.emptySet();
            return yaml.getConfigurationSection(base).getKeys(false);
        }

        String getString(String path, String def) {
            String full = base + "." + path;
            return yaml.contains(full) ? yaml.getString(full) : def;
        }

        List<String> getStringList(String path) {
            String full = base + "." + path;
            return yaml.getStringList(full);
        }
    }
}
