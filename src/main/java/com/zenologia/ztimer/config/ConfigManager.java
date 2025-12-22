package com.zenologia.ztimer.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.util.TimerIdNormalizer;

public class ConfigManager {

    private final ZTimerPlugin plugin;
    private FileConfiguration config;

    private int globalTopNDefault;
    private Map<String, Integer> perTimerTopN;
    private Map<String, Location> timerExitLocations;
    private Location fallbackExitLocation;
    private boolean fallbackExitEnabled;

    private String prefix;
    private String msgNoPermission;
    private String msgTimerNotRunning;
    private String msgInvalidTimerId;
    private String msgInvalidPlayerSelector;
    private String msgStart;
    private String msgStop;
    private String msgReset;
    private String msgCancel;
    private String msgReload;

    // New global reset messages (moved out of ZTimerCommand)
    private String msgResetConfirmGlobal;
    private String msgResetSuccessGlobal;

    private String timeDefault;
    private String timePattern;

    private boolean debugEnabled;
    private boolean debugLogStartStop;
    private boolean debugLogDbErrors;

    public ConfigManager(ZTimerPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.globalTopNDefault = config.getInt("leaderboards.global_top_n_default", 5);

        this.perTimerTopN = new HashMap<>();
        ConfigurationSection perTimerSection = config.getConfigurationSection("leaderboards.per_timer");
        if (perTimerSection != null) {
            for (String key : perTimerSection.getKeys(false)) {
                String normalizedId = TimerIdNormalizer.normalize(key);
                if (normalizedId != null && !normalizedId.isEmpty()) {
                    perTimerTopN.put(normalizedId, perTimerSection.getInt(key));
                }
            }
        }

        this.timerExitLocations = new HashMap<>();
        ConfigurationSection mazesSection = config.getConfigurationSection("mazes");
        if (mazesSection != null) {
            for (String key : mazesSection.getKeys(false)) {
                String normalizedId = TimerIdNormalizer.normalize(key);
                if (normalizedId == null || normalizedId.isEmpty()) {
                    plugin.getLogger().warning("Maze key '" + key + "' normalized to empty; skipping.");
                    continue;
                }
                ConfigurationSection exitSec = mazesSection.getConfigurationSection(key + ".exit_location");
                if (exitSec != null) {
                    Location loc = parseLocation(exitSec);
                    if (loc != null) {
                        timerExitLocations.put(normalizedId, loc);
                    } else {
                        plugin.getLogger().warning("Invalid exit location for maze '" + key + "'.");
                    }
                }
            }
        }

        ConfigurationSection fallbackSec = config.getConfigurationSection("fallback_exit_location");
        if (fallbackSec != null) {
            this.fallbackExitEnabled = fallbackSec.getBoolean("enabled", false);
            if (fallbackExitEnabled) {
                this.fallbackExitLocation = parseLocation(fallbackSec);
            } else {
                this.fallbackExitLocation = null;
            }
        } else {
            this.fallbackExitEnabled = false;
            this.fallbackExitLocation = null;
        }

        this.prefix = color(config.getString("messages.prefix", "&7[&bZTimer&7] "));
        this.msgNoPermission = color(config.getString("messages.errors.no_permission", "You do not have permission."));
        this.msgTimerNotRunning = color(config.getString("messages.errors.timer_not_running", "Timer &e%timer%&7 is not running for &b%player%&7."));
        this.msgInvalidTimerId = color(config.getString("messages.errors.invalid_timer_id", "Timer ID &e%timer%&7 is invalid."));
        this.msgInvalidPlayerSelector = color(config.getString("messages.errors.invalid_player_selector", "No valid players found for selector &e%selector%&7."));
        this.msgStart = color(config.getString("messages.info.start", "Started timer &e%timer%&7 for &b%player%&7."));
        this.msgStop = color(config.getString("messages.info.stop", "Stopped timer &e%timer%&7 for &b%player%&7. Time: &a%time%&7."));
        this.msgReset = color(config.getString("messages.info.reset", "Reset timer &e%timer%&7 for &b%player%&7."));
        this.msgCancel = color(config.getString("messages.info.cancel", "Canceled timer &e%timer%&7 for &b%player%&7."));
        this.msgReload = color(config.getString("messages.info.reload", "ZTimer configuration reloaded."));

        // Load newly moved global reset messages
        // Defaults include color codes around %timer% and %selector% so replacement of %selector% can be plain text (e.g. "all players")
        this.msgResetConfirmGlobal = color(config.getString("messages.info.reset_confirm_global",
                "This will reset all stored times for timer &e%timer%&7 for &b%selector%&7. Type &c/ztimer reset %timer% confirm&7 to confirm."));
        this.msgResetSuccessGlobal = color(config.getString("messages.info.reset_success_global",
                "Reset timer &e%timer%&7 for &b%selector%&7."));

        this.timeDefault = config.getString("formatting.time_default", "-");
        this.timePattern = config.getString("formatting.time_pattern", "mm:ss");

        this.debugEnabled = config.getBoolean("debug.enabled", false);
        this.debugLogStartStop = config.getBoolean("debug.log_start_stop", true);
        this.debugLogDbErrors = config.getBoolean("debug.log_db_errors", true);
    }

    private Location parseLocation(ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) return null;
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World '" + worldName + "' not found for location.");
            return null;
        }
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw");
        float pitch = (float) section.getDouble("pitch");
        return new Location(world, x, y, z, yaw, pitch);
    }

    private String color(String input) {
        return input == null ? "" : input.replace('&', '\u00A7');
    }

    public int getTopNForTimer(String timerId) {
        String normalized = TimerIdNormalizer.normalize(timerId);
        if (normalized == null) {
            return globalTopNDefault;
        }
        return perTimerTopN.getOrDefault(normalized, globalTopNDefault);
    }

    public Map<String, Integer> getPerTimerTopN() {
        return Collections.unmodifiableMap(perTimerTopN);
    }

    public Location getExitLocationForTimer(String timerId) {
        String normalized = TimerIdNormalizer.normalize(timerId);
        if (normalized == null) {
            return null;
        }
        Location loc = timerExitLocations.get(normalized);
        if (loc != null) {
            return loc.clone();
        }
        return null;
    }

    public boolean isFallbackExitEnabled() {
        return fallbackExitEnabled;
    }

    public Location getFallbackExitLocation() {
        return fallbackExitLocation == null ? null : fallbackExitLocation.clone();
    }

    public String getPrefix() {
        return prefix;
    }

    public String getMsgNoPermission() {
        return msgNoPermission;
    }

    public String getMsgTimerNotRunning() {
        return msgTimerNotRunning;
    }

    public String getMsgInvalidTimerId() {
        return msgInvalidTimerId;
    }

    public String getMsgInvalidPlayerSelector() {
        return msgInvalidPlayerSelector;
    }

    public String getMsgStart() {
        return msgStart;
    }

    public String getMsgStop() {
        return msgStop;
    }

    public String getMsgReset() {
        return msgReset;
    }

    public String getMsgCancel() {
        return msgCancel;
    }

    public String getMsgReload() {
        return msgReload;
    }

    // Getters for the moved global reset messages
    public String getMsgResetConfirmGlobal() {
        return msgResetConfirmGlobal;
    }

    public String getMsgResetSuccessGlobal() {
        return msgResetSuccessGlobal;
    }

    public String getTimeDefault() {
        return timeDefault;
    }

    public String getTimePattern() {
        return timePattern;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public boolean isDebugLogStartStop() {
        return debugLogStartStop;
    }

    public boolean isDebugLogDbErrors() {
        return debugLogDbErrors;
    }


    public Set<String> getKnownTimerIds() {
        Set<String> ids = new HashSet<>();
        ids.addAll(perTimerTopN.keySet());
        ids.addAll(timerExitLocations.keySet());
        return Collections.unmodifiableSet(ids);
    }


    public FileConfiguration getRawConfig() {
        return config;
    }

    /**
     * Returns relog-commands for a timer.
     * Checks both mazes.<id>.relog-commands and timers.<id>.relog-commands for compatibility.
     */
    public List<String> getRelogCommandsForTimer(String timerId) {
        if (timerId == null) return Collections.emptyList();
        String normalized = TimerIdNormalizer.normalize(timerId);

        // Check mazes.<id>.relog-commands first (existing repo structure)
        String mazesPath = "mazes." + timerId + ".relog-commands";
        if (config.contains(mazesPath)) {
            return config.getStringList(mazesPath);
        }
        if (normalized != null) {
            String mazesNorm = "mazes." + normalized + ".relog-commands";
            if (config.contains(mazesNorm)) {
                return config.getStringList(mazesNorm);
            }
        }

        // Fallback to timers.<id>.relog-commands
        String timersPath = "timers." + timerId + ".relog-commands";
        if (config.contains(timersPath)) {
            return config.getStringList(timersPath);
        }
        if (normalized != null) {
            String timersNorm = "timers." + normalized + ".relog-commands";
            if (config.contains(timersNorm)) {
                return config.getStringList(timersNorm);
            }
        }

        return Collections.emptyList();
    }

    /**
     * Returns logout-commands for a timer.
     * Checks both mazes.<id>.logout-commands and timers.<id>.logout-commands for compatibility.
     */
    public List<String> getLogoutCommandsForTimer(String timerId) {
        if (timerId == null) return Collections.emptyList();
        String normalized = TimerIdNormalizer.normalize(timerId);

        // Check mazes.<id>.logout-commands first
        String mazesPath = "mazes." + timerId + ".logout-commands";
        if (config.contains(mazesPath)) {
            return config.getStringList(mazesPath);
        }
        if (normalized != null) {
            String mazesNorm = "mazes." + normalized + ".logout-commands";
            if (config.contains(mazesNorm)) {
                return config.getStringList(mazesNorm);
            }
        }

        // Fallback to timers.<id>.logout-commands
        String timersPath = "timers." + timerId + ".logout-commands";
        if (config.contains(timersPath)) {
            return config.getStringList(timersPath);
        }
        if (normalized != null) {
            String timersNorm = "timers." + normalized + ".logout-commands";
            if (config.contains(timersNorm)) {
                return config.getStringList(timersNorm);
            }
        }

        return Collections.emptyList();
    }
}
