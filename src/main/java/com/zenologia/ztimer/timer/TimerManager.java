package com.zenologia.ztimer.timer;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;
import com.zenologia.ztimer.db.LeaderboardEntry;
import com.zenologia.ztimer.db.Storage;
import com.zenologia.ztimer.util.TimeFormatter;
import com.zenologia.ztimer.util.TimerIdNormalizer;

public class TimerManager {

    private final ZTimerPlugin plugin;
    private final Storage storage;
    private final ConfigManager configManager;
    private final PendingTeleportManager pendingTeleportManager;

    private final Map<UUID, ActiveTimer> activeTimers = new ConcurrentHashMap<>();

    // Cache of best times (ms) per player+timer
    private final Map<String, Long> bestTimeCache = new ConcurrentHashMap<>();

    // Leaderboard cache per timer
    private final Map<String, List<LeaderboardEntry>> leaderboardCache = new ConcurrentHashMap<>();

    public TimerManager(ZTimerPlugin plugin,
                        Storage storage,
                        ConfigManager configManager,
                        PendingTeleportManager pendingTeleportManager) {
        this.plugin = plugin;
        this.storage = storage;
        this.configManager = configManager;
        this.pendingTeleportManager = pendingTeleportManager;
    }

    private String cacheKey(UUID uuid, String timerId) {
        return uuid.toString() + "|" + timerId;
    }

    public void startTimer(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return;
        }

        // Any start cancels existing active timer (without recording)
        activeTimers.remove(player.getUniqueId());
        activeTimers.put(player.getUniqueId(), new ActiveTimer(timerId, System.currentTimeMillis()));

        if (configManager.isDebugEnabled() && configManager.isDebugLogStartStop()) {
            plugin.getLogger().info("Started timer '" + timerId + "' for " + player.getName());
        }
    }

    public Long stopTimer(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return null;
        }

        ActiveTimer active = activeTimers.get(player.getUniqueId());
        if (active == null || !active.getTimerId().equals(timerId)) {
            return null;
        }

        activeTimers.remove(player.getUniqueId());
        long elapsed = active.getElapsedMillis();
        long now = System.currentTimeMillis();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Long currentBest = storage.getBestTime(player.getUniqueId(), timerId);
                boolean improved = currentBest == null || elapsed < currentBest;
                if (improved) {
                    storage.updateBestTime(player.getUniqueId(), player.getName(), timerId, elapsed, now);
                    bestTimeCache.put(cacheKey(player.getUniqueId(), timerId), elapsed);
                    refreshLeaderboardCache(timerId);
                }
            } catch (Exception ex) {
                if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                    plugin.getLogger().severe("Error updating best time for " + player.getName() +
                            " timer '" + timerId + "': " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        if (configManager.isDebugEnabled() && configManager.isDebugLogStartStop()) {
            plugin.getLogger().info("Stopped timer '" + timerId + "' for " + player.getName() +
                    " elapsed=" + elapsed + "ms");
        }

        return elapsed;
    }

    public boolean resetTimer(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return false;
        }

        // Remove active timer if matching
        ActiveTimer active = activeTimers.get(player.getUniqueId());
        if (active != null && active.getTimerId().equals(timerId)) {
            activeTimers.remove(player.getUniqueId());
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storage.resetBestTime(player.getUniqueId(), timerId);
                bestTimeCache.remove(cacheKey(player.getUniqueId(), timerId));
                refreshLeaderboardCache(timerId);
            } catch (Exception ex) {
                if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                    plugin.getLogger().severe("Error resetting best time for " + player.getName() +
                            " timer '" + timerId + "': " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        return true;
    }

    public boolean cancelTimer(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return false;
        }

        ActiveTimer active = activeTimers.get(player.getUniqueId());
        if (active == null || !active.getTimerId().equals(timerId)) {
            return false;
        }

        activeTimers.remove(player.getUniqueId());

        teleportToExit(player, timerId);

        if (configManager.isDebugEnabled() && configManager.isDebugLogStartStop()) {
            plugin.getLogger().info("Canceled timer '" + timerId + "' for " + player.getName());
        }

        return true;
    }

    public void handleLogout(Player player) {
        ActiveTimer active = activeTimers.remove(player.getUniqueId());
        if (active != null) {
            // Determine relog commands from config (optional)
            List<String> relogCommands = configManager.getRelogCommandsForTimer(active.getTimerId());

            // Determine logout commands from config (optional) and run them immediately
            List<String> logoutCommands = configManager.getLogoutCommandsForTimer(active.getTimerId());
            if (logoutCommands != null && !logoutCommands.isEmpty()) {
                try {
                    // Run logout commands immediately on the main thread (player still available in event)
                    runRelogCommands(player, logoutCommands);
                } catch (Exception ex) {
                    if (configManager.isDebugEnabled()) {
                        plugin.getLogger().severe("Error running logout commands for " + player.getName() +
                                " timer '" + active.getTimerId() + "': " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }

            // Persist pending teleport (file-backed) with relog commands (unchanged)
            pendingTeleportManager.setPendingTeleport(player.getUniqueId(), active.getTimerId(), relogCommands);

            if (configManager.isDebugEnabled() && configManager.isDebugLogStartStop()) {
                plugin.getLogger().info("Player " + player.getName() +
                        " logged out during timer '" + active.getTimerId() + "', pending teleport set.");
            }
        }
    }

    public void handleJoin(Player player) {
        // Update stored player_name on every join
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                storage.updatePlayerName(player.getUniqueId(), player.getName());
            } catch (Exception ex) {
                if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                    plugin.getLogger().severe("Error updating player_name on join for " + player.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });

        PendingTeleport pt = pendingTeleportManager.consumePendingTeleport(player.getUniqueId());
        if (pt != null) {
            // teleport immediately on main thread
            Bukkit.getScheduler().runTask(plugin, () -> teleportToExit(player, pt.getTimerId()));

            // Run relog commands 1 tick later so player entity is fully initialized for commands targeting the player
            Bukkit.getScheduler().runTaskLater(plugin, () -> runRelogCommands(player, pt.getCommands()), 1L);
        }
    }

    private void teleportToExit(Player player, String timerId) {
        Location target = configManager.getExitLocationForTimer(timerId);

        if (target == null && configManager.isFallbackExitEnabled()) {
            target = configManager.getFallbackExitLocation();
        }

        if (target == null) {
            // Final fallback: overworld spawn (world "world" or first world), then player's world spawn
            World overworld = Bukkit.getWorld("world");
            if (overworld == null && !Bukkit.getWorlds().isEmpty()) {
                overworld = Bukkit.getWorlds().get(0);
            }
            if (overworld != null) {
                target = overworld.getSpawnLocation();
            } else {
                World world = player.getWorld();
                if (world != null) {
                    target = world.getSpawnLocation();
                }
            }
        }

        if (target != null) {
            player.teleport(target);
        }
    }

    private void runRelogCommands(Player player, List<String> commands) {
        if (commands == null || commands.isEmpty()) return;
        for (String rawCmd : commands) {
            if (rawCmd == null || rawCmd.trim().isEmpty()) continue;
            String cmd = rawCmd.replace("%player%", player.getName())
                    .replace("%player_uuid%", player.getUniqueId().toString());
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            } catch (Exception ex) {
                if (configManager.isDebugEnabled()) {
                    plugin.getLogger().severe("Error running relog/logout command for " + player.getName() + ": " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        }
    }

    public Long getCurrentElapsedMillis(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return null;
        }
        ActiveTimer active = activeTimers.get(player.getUniqueId());
        if (active == null || !active.getTimerId().equals(timerId)) {
            return null;
        }
        return active.getElapsedMillis();
    }

    public boolean isActive(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return false;
        }
        ActiveTimer active = activeTimers.get(player.getUniqueId());
        return active != null && active.getTimerId().equals(timerId);
    }

    public Long getBestTimeMillis(Player player, String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return null;
        }
        String key = cacheKey(player.getUniqueId(), timerId);
        Long cached = bestTimeCache.get(key);
        if (cached != null) {
            return cached;
        }

        try {
            Long dbValue = storage.getBestTime(player.getUniqueId(), timerId);
            if (dbValue != null) {
                bestTimeCache.put(key, dbValue);
            }
            return dbValue;
        } catch (Exception ex) {
            if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                plugin.getLogger().severe("Error fetching best time for " + player.getName() +
                        " timer '" + timerId + "': " + ex.getMessage());
                ex.printStackTrace();
            }
            return null;
        }
    }

    public String formatMillisOrDefault(Long millis) {
        if (millis == null) {
            return configManager.getTimeDefault();
        }
        return TimeFormatter.formatMillis(millis, configManager.getTimePattern());
    }

    public List<LeaderboardEntry> getLeaderboard(String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return Collections.emptyList();
        }
        List<LeaderboardEntry> cached = leaderboardCache.get(timerId);
        if (cached != null) {
            return cached;
        }

        int topN = configManager.getTopNForTimer(timerId);
        try {
            List<LeaderboardEntry> entries = storage.getTopN(timerId, topN);
            leaderboardCache.put(timerId, entries);
            return entries;
        } catch (Exception ex) {
            if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                plugin.getLogger().severe("Error loading leaderboard for timer '" + timerId + "': " + ex.getMessage());
                ex.printStackTrace();
            }
            return Collections.emptyList();
        }
    }

    public void clearCachesForTimer(String timerId) {
        if (timerId == null) {
            return;
        }
        String suffix = "|" + timerId;
        bestTimeCache.entrySet().removeIf(e -> e.getKey().endsWith(suffix));
        leaderboardCache.remove(timerId);
    }

    public void refreshLeaderboardCache(String rawTimerId) {
        String timerId = TimerIdNormalizer.normalize(rawTimerId);
        if (timerId == null) {
            return;
        }
        int topN = configManager.getTopNForTimer(timerId);
        try {
            List<LeaderboardEntry> entries = storage.getTopN(timerId, topN);
            leaderboardCache.put(timerId, entries);
        } catch (Exception ex) {
            if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                plugin.getLogger().severe("Error refreshing leaderboard for timer '" + timerId + "': " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
