package dev.chunkdoctor.report;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.RiskLevel;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;

public final class ReportService implements AutoCloseable {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> config;
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(8), task -> {
        Thread thread = new Thread(task, "ChunkDoctor-Reports");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.AbortPolicy());

    public ReportService(JavaPlugin plugin, Supplier<PluginConfig> config) {
        this.plugin = plugin;
        this.config = config;
    }

    public boolean export(List<ChunkAnalysisResult> immutableResults, String suffix,
                          Consumer<Path> success, Runnable failure) {
        List<ChunkAnalysisResult> results = List.copyOf(immutableResults);
        String pluginVersion = plugin.getPluginMeta().getVersion();
        String serverVersion = plugin.getServer().getVersion();
        try {
            executor.execute(() -> {
                try {
                    ReportDocument document = document(results, pluginVersion, serverVersion);
                    Path path = write(document, suffix);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (plugin.isEnabled()) {
                            success.accept(path);
                        }
                    });
                } catch (RuntimeException | IOException error) {
                    plugin.getLogger().log(Level.SEVERE, "JSON report export failed", error);
                    if (plugin.isEnabled()) {
                        plugin.getServer().getScheduler().runTask(plugin, failure);
                    }
                }
            });
            return true;
        } catch (RuntimeException rejected) {
            return false;
        }
    }

    private ReportDocument document(List<ChunkAnalysisResult> results, String pluginVersion, String serverVersion) {
        int low = 0;
        int medium = 0;
        int high = 0;
        int critical = 0;
        for (ChunkAnalysisResult result : results) {
            if (result.riskLevel() == RiskLevel.LOW) {
                low++;
            } else if (result.riskLevel() == RiskLevel.MEDIUM) {
                medium++;
            } else if (result.riskLevel() == RiskLevel.HIGH) {
                high++;
            } else {
                critical++;
            }
        }
        return new ReportDocument(Instant.now(), pluginVersion, serverVersion,
                new ReportDocument.Summary(results.size(), low, medium, high, critical), results);
    }

    private Path write(ReportDocument document, String suffix) throws IOException {
        Path dataRoot = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path reportRoot = dataRoot.resolve(config.get().export().directory()).normalize();
        if (!reportRoot.startsWith(dataRoot)) {
            throw new IOException("Resolved report directory escaped plugin data folder");
        }
        Files.createDirectories(reportRoot);
        String safeSuffix = suffix.replaceAll("[^A-Za-z0-9._-]", "_");
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(document.generatedAt()).replace(':', '-');
        Path target = reportRoot.resolve("chunkdoctor-" + safeSuffix + "-" + timestamp + ".json").normalize();
        if (!target.startsWith(reportRoot)) {
            throw new IOException("Resolved report path escaped report directory");
        }
        Path temporary = reportRoot.resolve("." + UUID.randomUUID() + ".tmp");
        String json = new JsonReportSerializer(config.get().export().prettyJson()).serialize(document);
        Files.writeString(temporary, json, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicUnsupported) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Report worker did not stop within three seconds.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
