package dev.chunkdoctor.analysis;

import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkSnapshot;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Bounded worker pipeline. Only immutable snapshots enter the executor.
 */
public final class AnalysisPipeline implements AutoCloseable {
    private final JavaPlugin plugin;
    private final ChunkAnalyzer analyzer;
    private final Consumer<ChunkAnalysisResult> globalResultConsumer;
    private final ThreadPoolExecutor executor;

    public AnalysisPipeline(JavaPlugin plugin, ChunkAnalyzer analyzer,
                            Consumer<ChunkAnalysisResult> globalResultConsumer,
                            int workerThreads, int maximumPending) {
        this.plugin = Objects.requireNonNull(plugin);
        this.analyzer = Objects.requireNonNull(analyzer);
        this.globalResultConsumer = Objects.requireNonNull(globalResultConsumer);
        AtomicInteger ids = new AtomicInteger();
        ThreadFactory factory = task -> {
            Thread thread = new Thread(task, "ChunkDoctor-Worker-" + ids.incrementAndGet());
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler((ignored, error) ->
                    plugin.getLogger().log(Level.SEVERE, "Uncaught analysis worker error", error));
            return thread;
        };
        this.executor = new ThreadPoolExecutor(workerThreads, workerThreads, 30, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(maximumPending), factory, new ThreadPoolExecutor.AbortPolicy());
    }

    public boolean submit(ChunkSnapshot snapshot) {
        return submit(snapshot, result -> {
        });
    }

    public boolean submit(ChunkSnapshot snapshot, Consumer<ChunkAnalysisResult> callback) {
        try {
            executor.execute(() -> {
                try {
                    ChunkAnalysisResult result = analyzer.analyze(snapshot);
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        if (!plugin.isEnabled()) {
                            return;
                        }
                        globalResultConsumer.accept(result);
                        callback.accept(result);
                    });
                } catch (RuntimeException error) {
                    plugin.getLogger().log(Level.WARNING, "Failed to analyze " + snapshot.key().display(), error);
                }
            });
            return true;
        } catch (RejectedExecutionException rejected) {
            return false;
        }
    }

    public int pendingCount() {
        return executor.getQueue().size();
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                plugin.getLogger().warning("Analysis workers did not stop within three seconds.");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
