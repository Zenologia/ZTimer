package com.zenologia.ztimer.placeholder;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;
import com.zenologia.ztimer.db.LeaderboardEntry;
import com.zenologia.ztimer.db.Storage;
import com.zenologia.ztimer.timer.TimerManager;
import com.zenologia.ztimer.util.TimerIdNormalizer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ZTimerExpansion extends PlaceholderExpansion {

    private final ZTimerPlugin plugin;
    private final TimerManager timerManager;
    private final Storage storage;
    private final ConfigManager configManager;

    public ZTimerExpansion(ZTimerPlugin plugin, TimerManager timerManager, Storage storage, ConfigManager configManager) {
        this.plugin = plugin;
        this.timerManager = timerManager;
        this.storage = storage;
        this.configManager = configManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ztimer";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Zenologia";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {

        // Only one global active placeholder: active_global_<timerId>
        // This works even when Player is null.
        if (params.startsWith("active_global_")) {
            String timerIdPart = params.substring("active_global_".length());
            return timerManager.isAnyActive(timerIdPart) ? "true" : "false";
        }

        // For all other placeholders we need a valid Player context.
        if (player == null) {
            return "";
        }

        // per-player active_<timerId>
        if (params.startsWith("active_")) {
            String timerIdPart = params.substring("active_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "false";
            }
            return timerManager.isActive(player, normalized) ? "true" : "false";
        }

        // current_* and best_*
        if (params.startsWith("current_")) {
            return handleCurrent(player, params.substring("current_".length()));
        }

        if (params.startsWith("best_")) {
            return handleBest(player, params.substring("best_".length()));
        }

        if (params.startsWith("top_")) {
            return handleTop(params.substring("top_".length()));
        }

        return null;
    }

    private String handleCurrent(Player player, String remainder) {
        // current_<timerId>
        // current_seconds_<timerId>
        // current_millis_<timerId>
        if (remainder.startsWith("seconds_")) {
            String timerIdPart = remainder.substring("seconds_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long elapsed = timerManager.getCurrentElapsedMillis(player, normalized);
            if (elapsed == null) {
                return "";
            }
            return String.valueOf(elapsed / 1000L);
        } else if (remainder.startsWith("millis_")) {
            String timerIdPart = remainder.substring("millis_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long elapsed = timerManager.getCurrentElapsedMillis(player, normalized);
            if (elapsed == null) {
                return "";
            }
            return String.valueOf(elapsed);
        } else {
            String timerIdPart = remainder;
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long elapsed = timerManager.getCurrentElapsedMillis(player, normalized);
            if (elapsed == null) {
                return configManager.getTimeDefault();
            }
            return timerManager.formatMillisOrDefault(elapsed);
        }
    }

    private String handleBest(Player player, String remainder) {
        // best_<timerId>, best_seconds_<timerId>, best_millis_<timerId>
        if (remainder.startsWith("seconds_")) {
            String timerIdPart = remainder.substring("seconds_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long millis = timerManager.getBestTimeMillis(player, normalized);
            if (millis == null) {
                return "";
            }
            return String.valueOf(millis / 1000L);
        } else if (remainder.startsWith("millis_")) {
            String timerIdPart = remainder.substring("millis_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long millis = timerManager.getBestTimeMillis(player, normalized);
            if (millis == null) {
                return "";
            }
            return String.valueOf(millis);
        } else {
            String timerIdPart = remainder;
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return configManager.getTimeDefault();
            }
            Long millis = timerManager.getBestTimeMillis(player, normalized);
            return timerManager.formatMillisOrDefault(millis);
        }
    }

    private String handleTop(String remainder) {
        // top_<position>_<timerId>_name or _time
        String[] parts = remainder.split("_", 3);
        if (parts.length < 3) {
            return "";
        }
        try {
            int position = Integer.parseInt(parts[0]);
            String timerIdPart = parts[1];
            String suffix = parts[2]; // name or time
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            List<LeaderboardEntry> leaderboard = timerManager.getLeaderboard(normalized);
            int idx = position - 1;
            if (idx < 0 || idx >= leaderboard.size()) {
                return "";
            }
            LeaderboardEntry entry = leaderboard.get(idx);
            if (suffix.equals("name")) {
                return entry.getPlayerName();
            } else if (suffix.equals("time")) {
                return timerManager.formatMillisOrDefault(entry.getBestMillis());
            } else {
                return "";
            }
        } catch (NumberFormatException ex) {
            return "";
        }
    }
}
