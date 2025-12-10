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
        if (player == null) {
            return "";
        }

        // active_<timerId>
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
                return configManager.getTimeDefault();
            }
            Long elapsed = timerManager.getCurrentElapsedMillis(player, normalized);
            if (elapsed == null) {
                return configManager.getTimeDefault();
            }
            return timerManager.formatMillisOrDefault(elapsed);
        }
    }

    private String handleBest(Player player, String remainder) {
        // best_<timerId>
        // best_seconds_<timerId>
        // best_millis_<timerId>
        if (remainder.startsWith("seconds_")) {
            String timerIdPart = remainder.substring("seconds_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long best = timerManager.getBestTimeMillis(player, normalized);
            if (best == null) {
                return "";
            }
            return String.valueOf(best / 1000L);
        } else if (remainder.startsWith("millis_")) {
            String timerIdPart = remainder.substring("millis_".length());
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return "";
            }
            Long best = timerManager.getBestTimeMillis(player, normalized);
            if (best == null) {
                return "";
            }
            return String.valueOf(best);
        } else {
            String timerIdPart = remainder;
            String normalized = TimerIdNormalizer.normalize(timerIdPart);
            if (normalized == null) {
                return configManager.getTimeDefault();
            }
            Long best = timerManager.getBestTimeMillis(player, normalized);
            if (best == null) {
                return configManager.getTimeDefault();
            }
            return timerManager.formatMillisOrDefault(best);
        }
    }

    private String handleTop(String remainder) {
        // Expected format now:
        // top_<position>_<timerId>_name
        // top_<position>_<timerId>_time
        // top_<position>_<timerId>_seconds
        // top_<position>_<timerId>_millis

        int lastUnderscore = remainder.lastIndexOf('_');
        if (lastUnderscore <= 0) {
            return "";
        }
        String field = remainder.substring(lastUnderscore + 1);
        String beforeField = remainder.substring(0, lastUnderscore);

        int firstUnderscore = beforeField.indexOf('_');
        if (firstUnderscore <= 0) {
            return "";
        }

        String positionPart = beforeField.substring(0, firstUnderscore);
        String timerIdPart = beforeField.substring(firstUnderscore + 1);

        int position;
        try {
            position = Integer.parseInt(positionPart);
        } catch (NumberFormatException ex) {
            return "";
        }

        if (position <= 0) {
            return "";
        }

        String normalized = TimerIdNormalizer.normalize(timerIdPart);
        if (normalized == null) {
            return "";
        }

        java.util.List<LeaderboardEntry> leaderboard = timerManager.getLeaderboard(normalized);
        if (position > leaderboard.size()) {
            return "";
        }

        LeaderboardEntry entry = leaderboard.get(position - 1);

        switch (field.toLowerCase()) {
            case "name":
                return entry.getPlayerName();
            case "time":
                return timerManager.formatMillisOrDefault(entry.getBestMillis());
            case "seconds":
                return String.valueOf(entry.getBestMillis() / 1000L);
            case "millis":
                return String.valueOf(entry.getBestMillis());
            default:
                return "";
        }
    }
}
