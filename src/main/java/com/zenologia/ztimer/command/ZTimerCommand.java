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
import com.zenologia.ztimer.timer.TimerStartResult;
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
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageBase());
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
                handleReload(sender);
                return true;
            default:
                sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageBase());
                return true;
        }
    }

    private void handleStart(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageStart());
            return;
        }

        String timerId = resolveConfiguredTimerId(sender, args[1]);
        if (timerId == null) {
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            TimerStartResult result = timerManager.startTimer(target, timerId);
            if (result == null) {
                sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", args[1]));
                continue;
            }

            switch (result.getType()) {
                case STARTED:
                    sender.sendMessage(configManager.getPrefix()
                            + configManager.getMsgStart()
                            .replace("%timer%", result.getTimerId())
                            .replace("%player%", target.getName()));
                    break;
                case ALREADY_RUNNING:
                    sender.sendMessage(configManager.getPrefix()
                            + configManager.getMsgTimerAlreadyRunning()
                            .replace("%timer%", result.getTimerId())
                            .replace("%player%", target.getName()));
                    break;
                case REPLACED:
                    sender.sendMessage(configManager.getPrefix()
                            + configManager.getMsgStartReplaced()
                            .replace("%timer%", result.getTimerId())
                            .replace("%previous_timer%", result.getPreviousTimerId())
                            .replace("%player%", target.getName()));
                    break;
                default:
                    break;
            }
        }
    }

    private void handleStop(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageStop());
            return;
        }

        String timerId = resolveConfiguredTimerId(sender, args[1]);
        if (timerId == null) {
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            Long elapsed = timerManager.stopTimer(target, timerId);
            if (elapsed == null) {
                sender.sendMessage(configManager.getPrefix()
                        + configManager.getMsgTimerNotRunning()
                        .replace("%timer%", timerId)
                        .replace("%player%", target.getName()));
                continue;
            }

            String formatted = timerManager.formatMillisOrDefault(elapsed);
            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgStop()
                    .replace("%timer%", timerId)
                    .replace("%player%", target.getName())
                    .replace("%time%", formatted));
        }
    }

    private void handleReset(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageReset());
            return;
        }

        String timerId = resolveConfiguredTimerId(sender, args[1]);
        if (timerId == null) {
            return;
        }

        if (args.length == 2) {
            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgResetConfirmGlobal()
                    .replace("%timer%", timerId)
                    .replace("%selector%", configManager.getLabelAllPlayers()));
            return;
        }

        if (args.length == 3 && args[2].equalsIgnoreCase("confirm")) {
            Bukkit.getScheduler().runTaskAsynchronously(ZTimerPlugin.getInstance(), () -> {
                try {
                    ZTimerPlugin.getInstance().getStorage().resetBestTimeForTimer(timerId);
                    ZTimerPlugin.getInstance().getTimerManager().clearCachesForTimer(timerId);
                    ZTimerPlugin.getInstance().getTimerManager().refreshLeaderboardCache(timerId);
                } catch (Exception ex) {
                    if (configManager.isDebugEnabled() && configManager.isDebugLogDbErrors()) {
                        ZTimerPlugin.getInstance().getLogger().severe("Error resetting all times for timer '" + timerId + "': " + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            });
            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgResetSuccessGlobal()
                    .replace("%timer%", timerId)
                    .replace("%selector%", configManager.getLabelAllPlayers()));
            return;
        }

        String selector = args[2];
        List<Player> targets = resolvePlayers(sender, selector);
        if (targets.isEmpty()) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidPlayerSelector().replace("%selector%", selector));
            return;
        }

        for (Player target : targets) {
            boolean reset = timerManager.resetTimer(target, timerId);
            if (!reset) {
                continue;
            }

            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgReset()
                    .replace("%timer%", timerId)
                    .replace("%player%", target.getName()));
        }
    }

    private void handleCancel(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgUsageCancel());
            return;
        }

        String timerId = resolveConfiguredTimerId(sender, args[1]);
        if (timerId == null) {
            return;
        }

        if (args.length == 2) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(configManager.getPrefix() + configManager.getMsgOnlyPlayersSelfCancel());
                return;
            }

            Player player = (Player) sender;
            if (!player.hasPermission("ztimer.cancel.self")) {
                sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
                return;
            }

            boolean cancelled = timerManager.cancelTimer(player, timerId);
            if (!cancelled) {
                sender.sendMessage(configManager.getPrefix()
                        + configManager.getMsgTimerNotRunning()
                        .replace("%timer%", timerId)
                        .replace("%player%", player.getName()));
                return;
            }

            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgCancel()
                    .replace("%timer%", timerId)
                    .replace("%player%", player.getName()));
            return;
        }

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
            boolean cancelled = timerManager.cancelTimer(target, timerId);
            if (!cancelled) {
                sender.sendMessage(configManager.getPrefix()
                        + configManager.getMsgTimerNotRunning()
                        .replace("%timer%", timerId)
                        .replace("%player%", target.getName()));
                continue;
            }

            sender.sendMessage(configManager.getPrefix()
                    + configManager.getMsgCancel()
                    .replace("%timer%", timerId)
                    .replace("%player%", target.getName()));
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("ztimer.admin")) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgNoPermission());
            return;
        }

        plugin.getConfigManager().reload();
        sender.sendMessage(configManager.getPrefix() + configManager.getMsgReload());
    }

    private String resolveConfiguredTimerId(CommandSender sender, String rawTimerId) {
        if (!configManager.isConfiguredTimerId(rawTimerId)) {
            sender.sendMessage(configManager.getPrefix() + configManager.getMsgInvalidTimerId().replace("%timer%", rawTimerId));
            return null;
        }

        return TimerIdNormalizer.normalize(rawTimerId);
    }

    private List<Player> resolvePlayers(CommandSender sender, String selector) {
        try {
            List<Entity> entities = Bukkit.selectEntities(sender, selector);
            List<Player> players = new ArrayList<>();
            for (Entity entity : entities) {
                if (entity instanceof Player) {
                    players.add((Player) entity);
                }
            }
            if (!players.isEmpty()) {
                return players;
            }
        } catch (IllegalArgumentException ignored) {
        }

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
        for (String option : options) {
            if (option.toLowerCase().startsWith(lower)) {
                result.add(option);
            }
        }
        return result;
    }

    private List<String> tabCompleteTimerIds(String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();
        for (String timerId : configManager.getKnownTimerIds()) {
            if (timerId.toLowerCase().startsWith(lower)) {
                result.add(timerId);
            }
        }
        return result;
    }

    private List<String> tabCompleteSelectors(String prefix) {
        List<String> result = new ArrayList<>();
        String lower = prefix.toLowerCase();

        String[] selectors = {"@a", "@p", "@r", "@s", "@e"};
        for (String selector : selectors) {
            if (selector.toLowerCase().startsWith(lower)) {
                result.add(selector);
            }
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            String name = player.getName();
            if (name.toLowerCase().startsWith(lower)) {
                result.add(name);
            }
        }

        return result;
    }
}
