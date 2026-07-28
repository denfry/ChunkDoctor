package dev.chunkdoctor.config;

import dev.chunkdoctor.scoring.RiskWeights;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record PluginConfig(
        Monitoring monitoring,
        Worlds worlds,
        ManualScan manualScan,
        DeepScan deepScan,
        Risk risk,
        Notifications notifications,
        Export export,
        Map<String, String> messages
) {
    public record Monitoring(
            boolean enabled,
            long intervalTicks,
            int chunksPerCycle,
            double maxMillisecondsPerTick,
            long rescanCooldownMillis,
            long resultExpirationMillis,
            double pauseBelowTps,
            double resumeAboveTps,
            int workerThreads,
            int maximumPendingAnalyses
    ) {
    }

    public record Worlds(boolean whitelist, Set<String> names) {
        public boolean allows(String worldName) {
            boolean listed = names.stream().anyMatch(name -> name.equalsIgnoreCase(worldName));
            return whitelist == listed;
        }
    }

    public record ManualScan(int maximumRadius, int maximumChunks) {
    }

    public record DeepScan(
            boolean enabled,
            int blocksPerTick,
            double maximumMillisecondsPerTick,
            int maximumDurationSeconds,
            int maximumConcurrentScans
    ) {
    }

    public record Risk(
            int mediumThreshold,
            int highThreshold,
            int criticalThreshold,
            RiskWeights weights,
            ExcessThresholds excess,
            double thresholdPenaltyFactor,
            double densityPenaltyFactor
    ) {
    }

    public record ExcessThresholds(
            int entities,
            int items,
            int villagers,
            int minecarts,
            int hoppers,
            int blockEntities,
            int redstoneComponents
    ) {
    }

    public record Notifications(
            boolean enabled,
            boolean criticalOnly,
            int significantIncrease,
            long cooldownMillis
    ) {
    }

    public record Export(String directory, boolean prettyJson) {
    }

    public static Map<String, String> defaultMessages() {
        return Map.ofEntries(
                Map.entry("prefix", "<dark_gray>[<gold>ChunkDoctor</gold>]</dark_gray> "),
                Map.entry("no-permission", "<red>Недостаточно прав.</red>"),
                Map.entry("player-only", "<red>Эта команда доступна только игроку.</red>"),
                Map.entry("unknown-command", "<red>Неизвестная подкоманда.</red>"),
                Map.entry("invalid-number", "<red>Некорректное число.</red>"),
                Map.entry("scan-started", "<green>Сканирование запущено.</green>"),
                Map.entry("no-loaded-chunks", "<yellow>Нет загруженных чанков.</yellow>"),
                Map.entry("config-reloaded", "<green>Конфигурация перечитана.</green>"),
                Map.entry("monitoring-started", "<green>Мониторинг запущен.</green>"),
                Map.entry("monitoring-stopped", "<yellow>Мониторинг остановлен.</yellow>"),
                Map.entry("clear-confirm", "<yellow>Повторите команду с аргументом confirm.</yellow>"),
                Map.entry("cache-cleared", "<green>Кэш очищен.</green>"),
                Map.entry("export-complete", "<green>Отчёт создан: <file></green>"),
                Map.entry("export-failed", "<red>Ошибка экспорта.</red>")
        );
    }

    public static List<String> messageKeys() {
        return List.copyOf(defaultMessages().keySet());
    }
}
