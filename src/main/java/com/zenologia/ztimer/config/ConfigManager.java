package com.zenologia.ztimer.config;

import java.io.File;
import java.io.IOException;
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
import org.bukkit.configuration.file.YamlConfiguration;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.util.TimerIdNormalizer;

public class ConfigManager {

    private final ZTimerPlugin plugin;
    private final File messagesFile;

    private FileConfiguration config;
    private YamlConfiguration messagesConfig;

    private int globalTopNDefault;
    private Map<String, Integer> perTimerTopN;
    private Map<String, Location> timerExitLocations;
    private Set<String> configuredTimerIds;
    private Location fallbackExitLocation;
    private boolean fallbackExitEnabled;

    private String prefix;
    private String labelAllPlayers;
    private String msgNoPermission;
    private String msgTimerNotRunning;
    private String msgInvalidTimerId;
    private String msgInvalidPlayerSelector;
    private String msgOnlyPlayersSelfCancel;
    private String msgStart;
    private String msgStartReplaced;
    private String msgTimerAlreadyRunning;
    private String msgStop;
    private String msgReset;
    private String msgCancel;
    private String msgReload;
    private String msgResetConfirmGlobal;
    private String msgResetSuccessGlobal;
    private String msgUsageBase;
    private String msgUsageStart;
    private String msgUsageStop;
    private String msgUsageReset;
    private String msgUsageCancel;

    private String timeDefault;
    private String timePattern;

    private boolean debugEnabled;
    private boolean debugLogStartStop;
    private boolean debugLogDbErrors;

    public ConfigManager(ZTimerPlugin plugin) {
        this.plugin = plugin;
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        loadConfiguredTimerIds();

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

                ConfigurationSection exitSection = mazesSection.getConfigurationSection(key + ".exit_location");
                if (exitSection == null) {
                    continue;
                }

                Location location = parseLocation(exitSection);
                if (location != null) {
                    timerExitLocations.put(normalizedId, location);
                } else {
                    plugin.getLogger().warning("Invalid exit location for maze '" + key + "'.");
                }
            }
        }

        ConfigurationSection fallbackSection = config.getConfigurationSection("fallback_exit_location");
        if (fallbackSection != null) {
            this.fallbackExitEnabled = fallbackSection.getBoolean("enabled", false);
            this.fallbackExitLocation = fallbackExitEnabled ? parseLocation(fallbackSection) : null;
        } else {
            this.fallbackExitEnabled = false;
            this.fallbackExitLocation = null;
        }

        loadMessages();

        this.timeDefault = config.getString("formatting.time_default", "-");
        this.timePattern = config.getString("formatting.time_pattern", "mm:ss");

        this.debugEnabled = config.getBoolean("debug.enabled", false);
        this.debugLogStartStop = config.getBoolean("debug.log_start_stop", true);
        this.debugLogDbErrors = config.getBoolean("debug.log_db_errors", true);
    }

    private void loadConfiguredTimerIds() {
        Set<String> ids = new HashSet<>();
        ConfigurationSection timersSection = config.getConfigurationSection("timers");
        if (timersSection != null) {
            for (String key : timersSection.getKeys(false)) {
                String normalizedId = TimerIdNormalizer.normalize(key);
                if (normalizedId == null || normalizedId.isEmpty()) {
                    plugin.getLogger().warning("Timer key '" + key + "' normalized to empty; skipping.");
                    continue;
                }
                ids.add(normalizedId);
            }
        }
        this.configuredTimerIds = ids;
    }

    private void loadMessages() {
        boolean createdMessagesFile = ensureMessagesFile();
        this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        if (createdMessagesFile) {
            migrateLegacyMessages();
            this.messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        }

        this.prefix = color(message("prefix", "&7[&bZTimer&7] "));
        this.labelAllPlayers = color(message("shared.all_players", "all players"));

        this.msgNoPermission = color(message("errors.no_permission", "You do not have permission."));
        this.msgTimerNotRunning = color(message("errors.timer_not_running",
                "Timer &e%timer%&7 is not running for &b%player%&7."));
        this.msgInvalidTimerId = color(message("errors.invalid_timer_id",
                "Timer ID &e%timer%&7 is not configured under timers."));
        this.msgInvalidPlayerSelector = color(message("errors.invalid_player_selector",
                "No valid players found for selector &e%selector%&7."));
        this.msgOnlyPlayersSelfCancel = color(message("errors.only_players_self_cancel",
                "Only players may self-cancel."));

        this.msgStart = color(message("info.start", "Started timer &e%timer%&7 for &b%player%&7."));
        this.msgStartReplaced = color(message("info.start_replaced",
                "Started timer &e%timer%&7 for &b%player%&7. Active timer &e%previous_timer%&7 was canceled."));
        this.msgTimerAlreadyRunning = color(message("info.timer_already_running",
                "Timer &e%timer%&7 is already running for &b%player%&7."));
        this.msgStop = color(message("info.stop", "Stopped timer &e%timer%&7 for &b%player%&7. Time: &a%time%&7."));
        this.msgReset = color(message("info.reset", "Reset timer &e%timer%&7 for &b%player%&7."));
        this.msgCancel = color(message("info.cancel", "Canceled timer &e%timer%&7 for &b%player%&7."));
        this.msgReload = color(message("info.reload", "ZTimer configuration reloaded."));
        this.msgResetConfirmGlobal = color(message("info.reset_confirm",
                "This will reset all stored times for timer &e%timer%&7 for &b%selector%&7. Type &c/ztimer reset %timer% confirm&7 to confirm."));
        this.msgResetSuccessGlobal = color(message("info.reset_success",
                "Reset timer &e%timer%&7 for &b%selector%&7."));

        this.msgUsageBase = color(message("usage.base", "/ztimer <start|stop|reset|cancel|reload> ..."));
        this.msgUsageStart = color(message("usage.start", "Usage: /ztimer start <timerId> <playerSelector>"));
        this.msgUsageStop = color(message("usage.stop", "Usage: /ztimer stop <timerId> <playerSelector>"));
        this.msgUsageReset = color(message("usage.reset", "Usage: /ztimer reset <timerId> [playerSelector|confirm]"));
        this.msgUsageCancel = color(message("usage.cancel", "Usage: /ztimer cancel <timerId> [playerSelector]"));
    }

    private boolean ensureMessagesFile() {
        if (messagesFile.exists()) {
            return false;
        }

        File parent = messagesFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        if (plugin.getResource("messages.yml") != null) {
            plugin.saveResource("messages.yml", false);
        }

        if (messagesFile.exists()) {
            return true;
        }

        YamlConfiguration emptyMessages = new YamlConfiguration();
        try {
            emptyMessages.save(messagesFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to create messages.yml: " + ex.getMessage());
        }
        return true;
    }

    private void migrateLegacyMessages() {
        ConfigurationSection legacyMessages = config.getConfigurationSection("messages");
        if (legacyMessages == null) {
            return;
        }

        copySection(legacyMessages, messagesConfig);
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to migrate legacy messages into messages.yml: " + ex.getMessage());
        }
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            Object value = source.get(key);
            if (value instanceof ConfigurationSection) {
                ConfigurationSection targetChild = target.getConfigurationSection(key);
                if (targetChild == null) {
                    targetChild = target.createSection(key);
                }
                copySection((ConfigurationSection) value, targetChild);
                continue;
            }
            target.set(key, value);
        }
    }

    private String message(String path, String defaultValue) {
        return messagesConfig.getString(path, defaultValue);
    }

    private Location parseLocation(ConfigurationSection section) {
        String worldName = section.getString("world");
        if (worldName == null) {
            return null;
        }

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

        Location location = timerExitLocations.get(normalized);
        return location == null ? null : location.clone();
    }

    public boolean isFallbackExitEnabled() {
        return fallbackExitEnabled;
    }

    public Location getFallbackExitLocation() {
        return fallbackExitLocation == null ? null : fallbackExitLocation.clone();
    }

    public boolean isConfiguredTimerId(String timerId) {
        String normalized = TimerIdNormalizer.normalize(timerId);
        return normalized != null && configuredTimerIds.contains(normalized);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getLabelAllPlayers() {
        return labelAllPlayers;
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

    public String getMsgOnlyPlayersSelfCancel() {
        return msgOnlyPlayersSelfCancel;
    }

    public String getMsgStart() {
        return msgStart;
    }

    public String getMsgStartReplaced() {
        return msgStartReplaced;
    }

    public String getMsgTimerAlreadyRunning() {
        return msgTimerAlreadyRunning;
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

    public String getMsgResetConfirmGlobal() {
        return msgResetConfirmGlobal;
    }

    public String getMsgResetSuccessGlobal() {
        return msgResetSuccessGlobal;
    }

    public String getMsgUsageBase() {
        return msgUsageBase;
    }

    public String getMsgUsageStart() {
        return msgUsageStart;
    }

    public String getMsgUsageStop() {
        return msgUsageStop;
    }

    public String getMsgUsageReset() {
        return msgUsageReset;
    }

    public String getMsgUsageCancel() {
        return msgUsageCancel;
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
        return Collections.unmodifiableSet(configuredTimerIds);
    }

    public FileConfiguration getRawConfig() {
        return config;
    }

    public List<String> getRelogCommandsForTimer(String timerId) {
        if (timerId == null) {
            return Collections.emptyList();
        }

        String normalized = TimerIdNormalizer.normalize(timerId);

        String timersPath = "timers." + timerId + ".relog-commands";
        if (config.contains(timersPath)) {
            return config.getStringList(timersPath);
        }

        if (normalized != null) {
            String normalizedTimersPath = "timers." + normalized + ".relog-commands";
            if (config.contains(normalizedTimersPath)) {
                return config.getStringList(normalizedTimersPath);
            }
        }

        String mazesPath = "mazes." + timerId + ".relog-commands";
        if (config.contains(mazesPath)) {
            return config.getStringList(mazesPath);
        }

        if (normalized != null) {
            String normalizedMazesPath = "mazes." + normalized + ".relog-commands";
            if (config.contains(normalizedMazesPath)) {
                return config.getStringList(normalizedMazesPath);
            }
        }

        return Collections.emptyList();
    }

    public List<String> getLogoutCommandsForTimer(String timerId) {
        if (timerId == null) {
            return Collections.emptyList();
        }

        String normalized = TimerIdNormalizer.normalize(timerId);

        String timersPath = "timers." + timerId + ".logout-commands";
        if (config.contains(timersPath)) {
            return config.getStringList(timersPath);
        }

        if (normalized != null) {
            String normalizedTimersPath = "timers." + normalized + ".logout-commands";
            if (config.contains(normalizedTimersPath)) {
                return config.getStringList(normalizedTimersPath);
            }
        }

        String mazesPath = "mazes." + timerId + ".logout-commands";
        if (config.contains(mazesPath)) {
            return config.getStringList(mazesPath);
        }

        if (normalized != null) {
            String normalizedMazesPath = "mazes." + normalized + ".logout-commands";
            if (config.contains(normalizedMazesPath)) {
                return config.getStringList(normalizedMazesPath);
            }
        }

        return Collections.emptyList();
    }
}
