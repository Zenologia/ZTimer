package com.zenologia.ztimer.config;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import com.zenologia.ztimer.ZTimerPlugin;
import com.zenologia.ztimer.util.TimerIdNormalizer;

final class ConfigSynchronizer {

    private static final int CURRENT_CONFIG_VERSION = 2;
    private static final DateTimeFormatter BACKUP_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String EMPTY_SECTION_MARKER = "__ztimer_empty_section__";

    private final ZTimerPlugin plugin;
    private final File dataFolder;
    private final File configFile;
    private final File messagesFile;

    private ConfigSynchronizer(ZTimerPlugin plugin) {
        this.plugin = plugin;
        this.dataFolder = plugin.getDataFolder();
        this.configFile = new File(dataFolder, "config.yml");
        this.messagesFile = new File(dataFolder, "messages.yml");
    }

    public static void synchronize(ZTimerPlugin plugin) {
        new ConfigSynchronizer(plugin).synchronizeInternal();
    }

    private void synchronizeInternal() {
        try {
            if (!dataFolder.exists() && !dataFolder.mkdirs()) {
                plugin.getLogger().warning("Could not create plugin data folder for config synchronization.");
                return;
            }

            if (!configFile.exists()) {
                return;
            }

            YamlConfiguration currentConfig = loadFileConfiguration(configFile);
            int currentVersion = currentConfig.getInt("config_version", 1);
            if (currentVersion > CURRENT_CONFIG_VERSION) {
                plugin.getLogger().warning("Config version " + currentVersion
                        + " is newer than this build supports. Skipping automatic config migration.");
                return;
            }

            YamlConfiguration defaultConfig = loadBundledConfiguration("config.yml");
            YamlConfiguration finalConfig = buildConfigTarget(currentConfig, defaultConfig);

            YamlConfiguration currentMessages = messagesFile.exists()
                    ? loadFileConfiguration(messagesFile)
                    : new YamlConfiguration();
            YamlConfiguration defaultMessages = loadBundledConfiguration("messages.yml");
            YamlConfiguration finalMessages = buildMessagesTarget(currentConfig, currentMessages, defaultMessages);

            FileWritePlan configPlan = createWritePlan(configFile, finalConfig);
            FileWritePlan messagesPlan = createWritePlan(messagesFile, finalMessages);
            if (!configPlan.shouldWrite && !messagesPlan.shouldWrite) {
                return;
            }

            String timestamp = LocalDateTime.now().format(BACKUP_STAMP);
            prepareWritePlan(configPlan, timestamp);
            prepareWritePlan(messagesPlan, timestamp);

            List<FileWritePlan> appliedPlans = new ArrayList<>();
            try {
                applyWritePlan(configPlan);
                if (configPlan.shouldWrite) {
                    appliedPlans.add(configPlan);
                }

                applyWritePlan(messagesPlan);
                if (messagesPlan.shouldWrite) {
                    appliedPlans.add(messagesPlan);
                }
            } catch (IOException ex) {
                rollbackAppliedPlans(appliedPlans);
                throw ex;
            } finally {
                cleanupTemp(configPlan);
                cleanupTemp(messagesPlan);
            }

            if (configPlan.shouldWrite) {
                plugin.getLogger().info("Synchronized config.yml to config version " + CURRENT_CONFIG_VERSION + ".");
            }
            if (messagesPlan.shouldWrite) {
                if (messagesPlan.targetExists) {
                    plugin.getLogger().info("Synchronized messages.yml with bundled defaults.");
                } else {
                    plugin.getLogger().info("Created messages.yml from bundled defaults.");
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to synchronize config files: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private YamlConfiguration buildConfigTarget(YamlConfiguration currentConfig, YamlConfiguration defaultConfig) {
        YamlConfiguration target = cloneConfiguration(defaultConfig);
        target.set("leaderboards.per_timer", null);
        target.set("mazes", null);
        target.set("timers", null);
        target.set("messages", null);

        copyLeafValues(currentConfig, target, path ->
                !path.equals("config_version")
                        && !isPathOrChild(path, "messages")
                        && !isPathOrChild(path, "mazes")
                        && !isPathOrChild(path, "timers"));
        copyEmptySections(currentConfig, target, path ->
                !path.equals("messages")
                        && !isPathOrChild(path, "messages")
                        && !path.equals("mazes")
                        && !isPathOrChild(path, "mazes")
                        && !path.equals("timers")
                        && !isPathOrChild(path, "timers"));

        LinkedHashSet<String> knownTimerIds = new LinkedHashSet<>();
        Map<String, String> mazeSources = new LinkedHashMap<>();
        Map<String, String> timerSources = new LinkedHashMap<>();

        copyCanonicalTimerSection(currentConfig, target, "mazes", "mazes", mazeSources, knownTimerIds, true);
        copyCanonicalTimerSection(currentConfig, target, "timers", "timers", timerSources, knownTimerIds, false);
        migrateLegacyMazeCommandLists(currentConfig, target, knownTimerIds);

        for (String timerId : knownTimerIds) {
            ensureSection(target, "timers." + timerId);
        }

        if (!currentConfig.contains("storage.type")) {
            target.set("storage.type", "sqlite");
        }

        target.set("config_version", CURRENT_CONFIG_VERSION);
        target.set("messages", null);
        return target;
    }

    private YamlConfiguration buildMessagesTarget(YamlConfiguration currentConfig,
                                                  YamlConfiguration currentMessages,
                                                  YamlConfiguration defaultMessages) {
        YamlConfiguration target = cloneConfiguration(defaultMessages);

        ConfigurationSection legacyMessages = currentConfig.getConfigurationSection("messages");
        if (legacyMessages != null) {
            copyLeafValues(legacyMessages, target, path -> true);
            copyEmptySections(legacyMessages, target, path -> true);
        }

        copyLeafValues(currentMessages, target, path -> true);
        copyEmptySections(currentMessages, target, path -> true);
        return target;
    }

    private void copyCanonicalTimerSection(YamlConfiguration sourceRoot,
                                           YamlConfiguration targetRoot,
                                           String sourcePath,
                                           String targetPath,
                                           Map<String, String> normalizedSources,
                                           Set<String> knownTimerIds,
                                           boolean stripLegacyCommands) {
        ConfigurationSection sourceSection = sourceRoot.getConfigurationSection(sourcePath);
        if (sourceSection == null) {
            return;
        }

        for (String rawKey : sourceSection.getKeys(false)) {
            String normalized = TimerIdNormalizer.normalize(rawKey);
            if (normalized == null) {
                plugin.getLogger().warning("Skipping " + sourcePath + "." + rawKey
                        + " during config migration because it normalizes to an empty timer ID.");
                continue;
            }

            String previousSource = normalizedSources.putIfAbsent(normalized, sourcePath + "." + rawKey);
            if (previousSource != null && !previousSource.equals(sourcePath + "." + rawKey)) {
                plugin.getLogger().warning("Multiple " + sourcePath + " entries normalize to timer ID '" + normalized
                        + "'. Later values may override earlier ones during migration.");
            }

            knownTimerIds.add(normalized);

            ConfigurationSection child = sourceSection.getConfigurationSection(rawKey);
            if (child == null) {
                continue;
            }

            for (String relativePath : child.getKeys(true)) {
                if (child.isConfigurationSection(relativePath)) {
                    ConfigurationSection nestedSection = child.getConfigurationSection(relativePath);
                    if (nestedSection != null && nestedSection.getKeys(false).isEmpty()) {
                        ensureSection(targetRoot, targetPath + "." + normalized + "." + relativePath);
                    }
                    continue;
                }

                if (stripLegacyCommands && isLegacyMazeCommandPath(relativePath)) {
                    continue;
                }

                Object value = child.get(relativePath);
                targetRoot.set(targetPath + "." + normalized + "." + relativePath, cloneValue(value));
            }
        }
    }

    private void migrateLegacyMazeCommandLists(YamlConfiguration sourceRoot,
                                               YamlConfiguration targetRoot,
                                               Set<String> knownTimerIds) {
        ConfigurationSection mazesSection = sourceRoot.getConfigurationSection("mazes");
        if (mazesSection == null) {
            return;
        }

        for (String rawKey : mazesSection.getKeys(false)) {
            String normalized = TimerIdNormalizer.normalize(rawKey);
            if (normalized == null) {
                continue;
            }

            knownTimerIds.add(normalized);

            ConfigurationSection mazeSection = mazesSection.getConfigurationSection(rawKey);
            if (mazeSection == null) {
                continue;
            }

            copyLegacyMazeCommandList(mazeSection, targetRoot, normalized, "relog-commands");
            copyLegacyMazeCommandList(mazeSection, targetRoot, normalized, "logout-commands");
        }
    }

    private void copyLegacyMazeCommandList(ConfigurationSection mazeSection,
                                           YamlConfiguration targetRoot,
                                           String timerId,
                                           String listPath) {
        String canonicalPath = "timers." + timerId + "." + listPath;
        if (targetRoot.contains(canonicalPath)) {
            return;
        }

        if (!mazeSection.contains(listPath)) {
            return;
        }

        List<String> commands = mazeSection.getStringList(listPath);
        targetRoot.set(canonicalPath, new ArrayList<>(commands));
    }

    private FileWritePlan createWritePlan(File targetFile, YamlConfiguration targetConfig) {
        boolean targetExists = targetFile.exists();
        boolean shouldWrite = !targetExists;
        if (!shouldWrite) {
            try {
                YamlConfiguration current = loadFileConfiguration(targetFile);
                shouldWrite = !flattenConfiguration(current).equals(flattenConfiguration(targetConfig));
            } catch (IOException | InvalidConfigurationException ex) {
                plugin.getLogger().warning("Could not compare " + targetFile.getName()
                        + " before sync. Rewriting it using bundled defaults.");
                shouldWrite = true;
            }
        }
        return new FileWritePlan(targetFile, targetConfig, targetExists, shouldWrite);
    }

    private void prepareWritePlan(FileWritePlan plan, String timestamp) throws IOException {
        if (!plan.shouldWrite) {
            return;
        }

        plan.tempFile = new File(dataFolder, plan.targetFile.getName() + ".tmp");
        targetToFile(plan.targetConfig, plan.tempFile);

        if (plan.targetExists) {
            plan.backupFile = new File(dataFolder, plan.targetFile.getName() + ".bak-" + timestamp);
            Files.copy(plan.targetFile.toPath(), plan.backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void applyWritePlan(FileWritePlan plan) throws IOException {
        if (!plan.shouldWrite) {
            return;
        }

        try {
            Files.move(plan.tempFile.toPath(), plan.targetFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException ex) {
            Files.move(plan.tempFile.toPath(), plan.targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        plan.applied = true;
    }

    private void rollbackAppliedPlans(List<FileWritePlan> plans) {
        for (int i = plans.size() - 1; i >= 0; i--) {
            FileWritePlan plan = plans.get(i);
            try {
                rollbackWritePlan(plan);
            } catch (IOException rollbackError) {
                plugin.getLogger().severe("Failed to roll back " + plan.targetFile.getName()
                        + " after a sync error: " + rollbackError.getMessage());
            }
        }
    }

    private void rollbackWritePlan(FileWritePlan plan) throws IOException {
        if (!plan.applied) {
            return;
        }

        if (plan.backupFile != null && plan.backupFile.exists()) {
            Files.copy(plan.backupFile.toPath(), plan.targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }

        Files.deleteIfExists(plan.targetFile.toPath());
    }

    private void cleanupTemp(FileWritePlan plan) {
        if (plan.tempFile == null) {
            return;
        }

        try {
            Files.deleteIfExists(plan.tempFile.toPath());
        } catch (IOException ignored) {
        }
    }

    private void copyLeafValues(ConfigurationSection source,
                                YamlConfiguration target,
                                PathPredicate predicate) {
        for (String path : source.getKeys(true)) {
            if (source.isConfigurationSection(path)) {
                continue;
            }
            if (!predicate.shouldCopy(path)) {
                continue;
            }
            target.set(path, cloneValue(source.get(path)));
        }
    }

    private void copyEmptySections(ConfigurationSection source,
                                   YamlConfiguration target,
                                   PathPredicate predicate) {
        for (String path : source.getKeys(true)) {
            if (!source.isConfigurationSection(path)) {
                continue;
            }
            if (!predicate.shouldCopy(path)) {
                continue;
            }

            ConfigurationSection nestedSection = source.getConfigurationSection(path);
            if (nestedSection != null && nestedSection.getKeys(false).isEmpty()) {
                ensureSection(target, path);
            }
        }
    }

    private YamlConfiguration cloneConfiguration(YamlConfiguration source) {
        YamlConfiguration clone = new YamlConfiguration();
        try {
            clone.loadFromString(source.saveToString());
        } catch (InvalidConfigurationException ex) {
            throw new IllegalStateException("Failed to clone YAML configuration.", ex);
        }
        return clone;
    }

    private YamlConfiguration loadBundledConfiguration(String resourcePath) throws IOException, InvalidConfigurationException {
        try (InputStream stream = plugin.getResource(resourcePath)) {
            if (stream == null) {
                throw new IOException("Missing bundled resource: " + resourcePath);
            }

            String contents = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(contents);
            return configuration;
        }
    }

    private YamlConfiguration loadFileConfiguration(File file) throws IOException, InvalidConfigurationException {
        YamlConfiguration configuration = new YamlConfiguration();
        if (!file.exists()) {
            return configuration;
        }

        String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        configuration.loadFromString(contents);
        return configuration;
    }

    private void targetToFile(YamlConfiguration configuration, File targetFile) throws IOException {
        if (targetFile.exists()) {
            Files.delete(targetFile.toPath());
        }
        configuration.save(targetFile);
    }

    private Map<String, Object> flattenConfiguration(ConfigurationSection section) {
        Map<String, Object> flattened = new LinkedHashMap<>();
        for (String path : section.getKeys(true)) {
            if (section.isConfigurationSection(path)) {
                ConfigurationSection nestedSection = section.getConfigurationSection(path);
                if (nestedSection != null && nestedSection.getKeys(false).isEmpty()) {
                    flattened.put(path, EMPTY_SECTION_MARKER);
                }
                continue;
            }
            flattened.put(path, cloneValue(section.get(path)));
        }
        return flattened;
    }

    private Object cloneValue(Object value) {
        if (value instanceof List<?>) {
            return new ArrayList<>((List<?>) value);
        }
        return value;
    }

    private void ensureSection(YamlConfiguration configuration, String path) {
        if (configuration.isConfigurationSection(path)) {
            return;
        }
        configuration.createSection(path);
    }

    private boolean isPathOrChild(String path, String root) {
        return path.equals(root) || path.startsWith(root + ".");
    }

    private boolean isLegacyMazeCommandPath(String path) {
        return path.equals("relog-commands") || path.equals("logout-commands");
    }

    private interface PathPredicate {
        boolean shouldCopy(String path);
    }

    private static final class FileWritePlan {
        private final File targetFile;
        private final YamlConfiguration targetConfig;
        private final boolean targetExists;
        private final boolean shouldWrite;

        private File tempFile;
        private File backupFile;
        private boolean applied;

        private FileWritePlan(File targetFile,
                              YamlConfiguration targetConfig,
                              boolean targetExists,
                              boolean shouldWrite) {
            this.targetFile = targetFile;
            this.targetConfig = targetConfig;
            this.targetExists = targetExists;
            this.shouldWrite = shouldWrite;
        }
    }
}
