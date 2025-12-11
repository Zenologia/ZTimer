package com.zenologia.ztimer.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.config.ConfigManager;
import com.zenologia.ztimer.timer.TimerManager;
import com.zenologia.ztimer.util.TimerIdNormalizer;

public class ZTimerCommand implements CommandExecutor, TabCompleter {

    private final ZTimerPlugin plugin;
    private final TimerManager timerManager;
    private final ConfigManager configManager;

    public ZTimerCommand(ZTimerPlugin plugin, TimerManager timerManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.timerManager = timerManager;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(configManager.getPrefix() + "/ztimer <start|stop|reset|cancel|reload> ...");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "start":
                handleStart(sender, args);
                return true;
            case "stop":
                handleStop(sender, args);
                return true;
            case "reset":
                handleReset(sender, args);
                return true;
            case "cancel":
                handleCancel(sender, args);
                return true;
            case "reload":
                handleReload(sender, args);
                return true;
            default:
                sender.sendMessage(configManager.getPrefix() + "/ztimer <start|stop|reset|cancel|reload> ...");
                return true;
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(configManager.getPrefix() + "Usage: /ztimer start <timerId> <playerSelector>");
            return;
        }

        String rawTimerId = args[1];
        String normalized = TimerIdNormalizer.normalize(rawTimerId);
        if (normalized == null) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", rawTimerId));
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            timerManager.startTimer(target, normalized);
            sender.sendMessage(configManager.getPrefix() +
                    configManager.getMsgStart()
                            .replace("%timer%", normalized)
                            .replace("%player%", target.getName()));
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(configManager.getPrefix() + "Usage: /ztimer stop <timerId> <playerSelector>");
            return;
        }

        String rawTimerId = args[1];
        String normalized = TimerIdNormalizer.normalize(rawTimerId);
        if (normalized == null) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", rawTimerId));
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            Long elapsed = timerManager.stopTimer(target, normalized);
            if (elapsed == null) {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgTimerNotRunning()
                                .replace("%timer%", normalized)
                                .replace("%player%", target.getName()));
            } else {
                String formatted = timerManager.formatMillisOrDefault(elapsed);
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgStop()
                                .replace("%timer%", normalized)
                                .replace("%player%", target.getName())
                                .replace("%time%", formatted));
            }
        }
    }


private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "Usage: /ztimer reset <timerId> [playerSelector|confirm]");
            return;
        }

        String rawTimerId = args[1];
        String normalized = TimerIdNormalizer.normalize(rawTimerId);
        if (normalized == null) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", rawTimerId));
            return;
        }

        // Global reset confirmation: /ztimer reset <timerId>
        if (args.length == 2) {
            sender.sendMessage(configManager.getPrefix() +
                    configManager.getMsgResetConfirmGlobal()
                            .replace("%timer%", normalized)
                            .replace("%selector%", "all players"));
            return;
        }

        // Global reset execution: /ztimer reset <timerId> confirm
        if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
            Bukkit.getScheduler().runTaskAsynchronously(ZTimerPlugin.getInstance(), () -> {
                try {
                    ZTimerPlugin.getInstance().getStorage().resetBestTimeForTimer(normalized);
                    ZTimerPlugin.getInstance().getTimerManager().clearCachesForTimer(normalized);
                    ZTimerPlugin.getInstance().getTimerManager().refreshLeaderboardCache(normalized);
                } catch (Exception ex) {
                    if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                        ZTimerPlugin.getInstance().getLogger().severe("Error resetting all times for timer '" + normalized + "': " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
            sender.sendMessage(configManager.getPrefix() +
                    configManager.getMsgResetSuccessGlobal()
                            .replace("%timer%", normalized)
                            .replace("%selector%", "all players"));
            return;
        }

        // Per-player reset: /ztimer reset <timerId> <playerSelector>
        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            boolean ok = timerManager.resetTimer(target, normalized);
            if (ok) {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgReset()
                                .replace("%timer%", normalized)
                                .replace("%player%", target.getName()));
            }
        }
    }


private void handleCancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + "Usage: /ztimer cancel <timerId> [playerSelector]");
            return;
        }

        String rawTimerId = args[1];
        String normalized = TimerIdNormalizer.normalize(rawTimerId);
        if (normalized == null) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", rawTimerId));
            return;
        }

        if (args.length == 2) {
            // Self cancel
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getPrefix() + "Only players may self-cancel.");
                return;
            }
            Player player = (Player) sender;
            if (!player.hasPermission("ztimer.cancel.self")) {
                sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
                return;
            }
            boolean cancelled = timerManager.cancelTimer(player, normalized);
            if (!cancelled) {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgTimerNotRunning()
                                .replace("%timer%", normalized)
                                .replace("%player%", player.getName()));
            } else {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgCancel()
                                .replace("%timer%", normalized)
                                .replace("%player%", player.getName()));
            }
            return;
        }

        // Cancel others
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            boolean cancelled = timerManager.cancelTimer(target, normalized);
            if (!cancelled) {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgTimerNotRunning()
                                .replace("%timer%", normalized)
                                .replace("%player%", target.getName()));
            } else {
                sender.sendMessage(configManager.getPrefix() +
                        configManager.getMsgCancel()
                                .replace("%timer%", normalized)
                                .replace("%player%", target.getName()));
            }
        }
    }

    private void handleReload(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }
        plugin.getConfigManager().reload();
        sender.sendMessage(configManager.getPrefix() + configManager.getMsgReload());
    }

    private List<Player> resolvePlayers(CommandSender sender, String selector) {
        // Try Bukkit entity selector first (@p, @a, player name, etc.)
        try {
            List<Entity> entities = Bukkit.selectEntities(sender, selector);
            List<Player> players = new ArrayList<>();
            for (Entity e : entities) {
                if (e instanceof Player) {
                    players.add((Player) e);
                }
            }
            if (!players.isEmpty()) {
                return players;
            }
        } catch (IllegalArgumentException ignored) {
        }

        // Fallback: single player by exact name
        Player player = Bukkit.getPlayerExact(selector);
        if (player != null) {
            return Collections.singletonList(player);
        }

        return Collections.emptyList();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subs = new ArrayList<>();
            subs.add("start");
            subs.add("stop");
            subs.add("reset");
            subs.add("cancel");
            subs.add("reload");
            return partial(subs, args[0]);
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("start") || sub.equals("stop") || sub.equals("reset") || sub.equals("cancel")) {
                return tabCompleteTimerIds(args[1]);
            }
        }

        if (args.length == 3) {
            String sub = args[0].toLowerCase();
            if (sub.equals("start") || sub.equals("stop") || sub.equals("reset") || sub.equals("cancel")) {
                // For reset, third argument can be player selector OR 'confirm'
                if (sub.equals("reset")) {
                    List<String> options = new ArrayList<>();
                    options.add("confirm");
                    options.addAll(tabCompleteSelectors(args[2]));
                    return partial(options, args[2]);
                }
                return tabCompleteSelectors(args[2]);
            }
        }

        return Collections.emptyList();
    }

    private List<String> partial(List<String> options, String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String opt : options) {
            if (opt.toLowerCase().startsWith(lower)) {
                result.add(opt);
            }
        }
        return result;
    }

    private List<String> tabCompleteTimerIds(String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String id : configManager.getKnownTimerIds()) {
            if (id.toLowerCase().startsWith(lower)) {
                result.add(id);
            }
        }
        return result;
    }

    private List<String> tabCompleteSelectors(String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();

        String[] selectors = {"@a", "@p", "@r", "@s", "@e"};
        for (String sel : selectors) {
            if (sel.toLowerCase().startsWith(lower)) {
                result.add(sel);
            }
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            String name = p.getName();
            if (name.toLowerCase().startsWith(lower)) {
                result.add(name);
            }
        }
        return result;
    }
}
