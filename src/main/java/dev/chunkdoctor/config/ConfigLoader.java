package dev.chunkdoctor.config;

import dev.chunkdoctor.scoring.RiskWeights;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ConfigLoader {
    private final Consumer<String> warningSink;

    public ConfigLoader(Consumer<String> warningSink) {
        this.warningSink = warningSink;
    }

    public PluginConfig load(FileConfiguration config) {
        PluginConfig.Monitoring monitoring = new PluginConfig.Monitoring(
                config.getBoolean("monitoring.enabled", true),
                boundedLong(config, "monitoring.interval-ticks", 100, 1, 72_000),
                boundedInt(config, "monitoring.chunks-per-cycle", 2, 1, 256),
                boundedDouble(config, "monitoring.max-milliseconds-per-tick", 2.0, 0.1, 25.0),
                boundedLong(config, "monitoring.rescan-cooldown-seconds", 300, 1, 86_400) * 1_000,
                boundedLong(config, "monitoring.result-expiration-minutes", 60, 1, 10_080) * 60_000,
                boundedDouble(config, "monitoring.pause-below-tps", 17.0, 1.0, 20.0),
                boundedDouble(config, "monitoring.resume-above-tps", 18.5, 1.0, 20.0),
                boundedInt(config, "monitoring.worker-threads", 2, 1, 8),
                boundedInt(config, "monitoring.maximum-pending-analyses", 64, 1, 2_048)
        );
        if (monitoring.resumeAboveTps() <= monitoring.pauseBelowTps()) {
            warningSink.accept("monitoring.resume-above-tps must exceed pause-below-tps; using a safe hysteresis.");
            monitoring = new PluginConfig.Monitoring(
                    monitoring.enabled(), monitoring.intervalTicks(), monitoring.chunksPerCycle(),
                    monitoring.maxMillisecondsPerTick(), monitoring.rescanCooldownMillis(),
                    monitoring.resultExpirationMillis(), monitoring.pauseBelowTps(),
                    Math.min(20.0, monitoring.pauseBelowTps() + 0.5), monitoring.workerThreads(),
                    monitoring.maximumPendingAnalyses());
        }

        String mode = config.getString("worlds.mode", "blacklist");
        boolean whitelist = "whitelist".equalsIgnoreCase(mode);
        if (!whitelist && !"blacklist".equalsIgnoreCase(mode)) {
            warningSink.accept("worlds.mode must be blacklist or whitelist; using blacklist.");
        }
        Set<String> worlds = new HashSet<>();
        config.getStringList("worlds.list").stream()
                .filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .forEach(worlds::add);

        PluginConfig.ManualScan manual = new PluginConfig.ManualScan(
                boundedInt(config, "manual-scan.maximum-radius", 8, 0, 32),
                boundedInt(config, "manual-scan.maximum-chunks", 128, 1, 4_225));
        PluginConfig.DeepScan deep = new PluginConfig.DeepScan(
                config.getBoolean("deep-scan.enabled", true),
                boundedInt(config, "deep-scan.blocks-per-tick", 2_048, 64, 65_536),
                boundedDouble(config, "deep-scan.maximum-milliseconds-per-tick", 3.0, 0.1, 25.0),
                boundedInt(config, "deep-scan.maximum-duration-seconds", 30, 1, 300),
                boundedInt(config, "deep-scan.maximum-concurrent-scans", 1, 1, 8));

        int[] thresholds = ConfigValidator.orderedThresholds(
                config.getInt("risk.thresholds.medium", 30),
                config.getInt("risk.thresholds.high", 60),
                config.getInt("risk.thresholds.critical", 80));
        RiskWeights weights = new RiskWeights(
                weight(config, "entities", 0.2), weight(config, "item-entities", 0.4),
                weight(config, "villagers", 2.5), weight(config, "ai-mobs", 0.8),
                weight(config, "minecarts", 1.5), weight(config, "boats", 0.6),
                weight(config, "item-frames", 0.35), weight(config, "hoppers", 1.2),
                weight(config, "active-hoppers", 0.4), weight(config, "furnaces", 0.7),
                weight(config, "active-furnaces", 0.8), weight(config, "pistons", 1.0),
                weight(config, "observers", 1.0), weight(config, "repeaters", 0.65),
                weight(config, "comparators", 0.75), weight(config, "redstone", 0.2),
                weight(config, "active-redstone", 0.5),
                weight(config, "spawners", 4.0), weight(config, "containers", 0.35),
                weight(config, "block-entities", 0.15));
        PluginConfig.ExcessThresholds excess = new PluginConfig.ExcessThresholds(
                excess(config, "entities", 100), excess(config, "items", 64),
                excess(config, "villagers", 24), excess(config, "minecarts", 20),
                excess(config, "hoppers", 32), excess(config, "block-entities", 64),
                excess(config, "redstone-components", 96));
        PluginConfig.Risk risk = new PluginConfig.Risk(
                thresholds[0], thresholds[1], thresholds[2], weights, excess,
                boundedDouble(config, "risk.threshold-penalty-factor", 0.075, 0.0, 10.0),
                boundedDouble(config, "risk.density-penalty-factor", 8.0, 0.0, 50.0));
        PluginConfig.Notifications notifications = new PluginConfig.Notifications(
                config.getBoolean("notifications.enabled", true),
                config.getBoolean("notifications.critical-only", true),
                boundedInt(config, "notifications.significant-increase", 15, 1, 100),
                boundedLong(config, "notifications.cooldown-seconds", 300, 1, 86_400) * 1_000);
        String exportDirectory = ConfigValidator.safeRelativeDirectory(
                config.getString("export.directory", "reports"));
        if (!exportDirectory.equals(config.getString("export.directory", "reports"))) {
            warningSink.accept("Unsafe export.directory rejected; using reports.");
        }
        PluginConfig.Export export = new PluginConfig.Export(
                exportDirectory, config.getBoolean("export.pretty-json", true));
        Map<String, String> messages = new HashMap<>(PluginConfig.defaultMessages());
        for (String key : PluginConfig.messageKeys()) {
            messages.put(key, config.getString("messages." + key, messages.get(key)));
        }
        return new PluginConfig(monitoring, new PluginConfig.Worlds(whitelist, Set.copyOf(worlds)),
                manual, deep, risk, notifications, export, Map.copyOf(messages));
    }

    private int boundedInt(FileConfiguration config, String path, int fallback, int min, int max) {
        int raw = config.getInt(path, fallback);
        int safe = ConfigValidator.clampInt(raw, min, max);
        warnChanged(path, raw, safe);
        return safe;
    }

    private long boundedLong(FileConfiguration config, String path, long fallback, long min, long max) {
        long raw = config.getLong(path, fallback);
        long safe = ConfigValidator.clampLong(raw, min, max);
        warnChanged(path, raw, safe);
        return safe;
    }

    private double boundedDouble(FileConfiguration config, String path, double fallback, double min, double max) {
        double raw = config.getDouble(path, fallback);
        double safe = ConfigValidator.clampDouble(raw, min, max);
        warnChanged(path, raw, safe);
        return safe;
    }

    private double weight(FileConfiguration config, String name, double fallback) {
        return boundedDouble(config, "risk.weights." + name, fallback, 0.0, 100.0);
    }

    private int excess(FileConfiguration config, String name, int fallback) {
        return boundedInt(config, "risk.excess-thresholds." + name, fallback, 1, 100_000);
    }

    private void warnChanged(String path, Number raw, Number safe) {
        if (Double.compare(raw.doubleValue(), safe.doubleValue()) != 0) {
            warningSink.accept(path + "=" + raw + " is outside the safe range; using " + safe + ".");
        }
    }
}
