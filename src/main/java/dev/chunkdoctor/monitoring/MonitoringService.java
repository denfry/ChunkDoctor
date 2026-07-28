package dev.chunkdoctor.monitoring;

import dev.chunkdoctor.analysis.AnalysisPipeline;
import dev.chunkdoctor.analysis.ChunkSnapshotCollector;
import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.ChunkKey;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Fair, time-budgeted traversal of loaded chunks. It never asks a world to load a chunk.
 */
public final class MonitoringService {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> config;
    private final ChunkSnapshotCollector collector;
    private final AnalysisPipeline pipeline;
    private final ResultRepository repository;
    private final Map<ChunkKey, Long> lastScans = new HashMap<>();
    private final Map<UUID, Integer> worldCursors = new HashMap<>();
    private BukkitTask task;
    private int worldCursor;
    private boolean tpsPaused;

    public MonitoringService(JavaPlugin plugin, Supplier<PluginConfig> config,
                             ChunkSnapshotCollector collector, AnalysisPipeline pipeline,
                             ResultRepository repository) {
        this.plugin = plugin;
        this.config = config;
        this.collector = collector;
        this.pipeline = pipeline;
        this.repository = repository;
    }

    public void start() {
        stop();
        PluginConfig.Monitoring settings = config.get().monitoring();
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::cycle,
                settings.intervalTicks(), settings.intervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        tpsPaused = false;
    }

    public void restartIfEnabled() {
        stop();
        if (config.get().monitoring().enabled()) {
            start();
        }
    }

    public boolean isRunning() {
        return task != null && !task.isCancelled();
    }

    public boolean isTpsPaused() {
        return tpsPaused;
    }

    public int trackedCooldowns() {
        return lastScans.size();
    }

    private void cycle() {
        PluginConfig settings = config.get();
        double tps = plugin.getServer().getTPS()[0];
        if (tpsPaused) {
            if (tps < settings.monitoring().resumeAboveTps()) {
                return;
            }
            tpsPaused = false;
        } else if (tps < settings.monitoring().pauseBelowTps()) {
            tpsPaused = true;
            return;
        }

        repository.removeExpired(settings.monitoring().resultExpirationMillis());
        long now = System.currentTimeMillis();
        lastScans.entrySet().removeIf(entry ->
                now - entry.getValue() > settings.monitoring().resultExpirationMillis());
        long deadline = System.nanoTime()
                + (long) (settings.monitoring().maxMillisecondsPerTick() * 1_000_000.0);
        int submitted = 0;
        List<World> worlds = plugin.getServer().getWorlds().stream()
                .filter(world -> settings.worlds().allows(world.getName()))
                .toList();
        if (worlds.isEmpty()) {
            return;
        }

        int worldsVisited = 0;
        while (submitted < settings.monitoring().chunksPerCycle()
                && System.nanoTime() < deadline
                && worldsVisited < worlds.size()) {
            World world = worlds.get(Math.floorMod(worldCursor++, worlds.size()));
            worldsVisited++;
            Chunk[] loaded = world.getLoadedChunks();
            if (loaded.length == 0) {
                continue;
            }
            int cursor = Math.floorMod(worldCursors.getOrDefault(world.getUID(), 0), loaded.length);
            int inspected = 0;
            while (inspected < loaded.length
                    && submitted < settings.monitoring().chunksPerCycle()
                    && System.nanoTime() < deadline) {
                Chunk chunk = loaded[cursor];
                cursor = (cursor + 1) % loaded.length;
                inspected++;
                ChunkKey key = new ChunkKey(world.getUID(), world.getName(), chunk.getX(), chunk.getZ());
                if (now - lastScans.getOrDefault(key, 0L) < settings.monitoring().rescanCooldownMillis()) {
                    continue;
                }
                try {
                    if (pipeline.submit(collector.collectQuick(chunk))) {
                        lastScans.put(key, now);
                        submitted++;
                    } else {
                        return;
                    }
                } catch (IllegalStateException race) {
                    plugin.getLogger().log(Level.FINE, "Chunk unloaded during passive collection", race);
                }
            }
            worldCursors.put(world.getUID(), cursor);
        }
    }
}
