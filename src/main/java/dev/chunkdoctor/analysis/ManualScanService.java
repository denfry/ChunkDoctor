package dev.chunkdoctor.analysis;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tick-sliced quick scans for player requests. Coordinates are admitted only if
 * loaded, then rechecked immediately before collection.
 */
public final class ManualScanService {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> config;
    private final ChunkSnapshotCollector collector;
    private final AnalysisPipeline pipeline;
    private final Map<UUID, ScanTask> active = new HashMap<>();

    public ManualScanService(JavaPlugin plugin, Supplier<PluginConfig> config,
                             ChunkSnapshotCollector collector, AnalysisPipeline pipeline) {
        this.plugin = plugin;
        this.config = config;
        this.collector = collector;
        this.pipeline = pipeline;
    }

    public StartResult start(Player player, int radius, Consumer<ChunkAnalysisResult> callback,
                             Runnable complete) {
        if (active.containsKey(player.getUniqueId())) {
            return new StartResult(false, 0);
        }
        World world = player.getWorld();
        int centerX = player.getLocation().getChunk().getX();
        int centerZ = player.getLocation().getChunk().getZ();
        int maximum = config.get().manualScan().maximumChunks();
        List<Coordinate> admitted = new ArrayList<>();
        outer:
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                if (admitted.size() >= maximum) {
                    break outer;
                }
                if (world.isChunkLoaded(x, z)) {
                    admitted.add(new Coordinate(x, z));
                }
            }
        }
        if (admitted.isEmpty()) {
            return new StartResult(true, 0);
        }
        ScanTask task = new ScanTask(player.getUniqueId(), world, List.copyOf(admitted), callback, complete);
        active.put(player.getUniqueId(), task);
        task.runTaskTimer(plugin, 1L, 1L);
        return new StartResult(true, admitted.size());
    }

    public int activeCount() {
        return active.size();
    }

    public void cancelWorld(World world) {
        active.entrySet().removeIf(entry -> {
            if (entry.getValue().world.getUID().equals(world.getUID())) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });
    }

    public void cancelAll() {
        active.values().forEach(BukkitRunnable::cancel);
        active.clear();
    }

    private final class ScanTask extends BukkitRunnable {
        private final UUID owner;
        private final World world;
        private final List<Coordinate> coordinates;
        private final Consumer<ChunkAnalysisResult> callback;
        private final Runnable complete;
        private int index;

        private ScanTask(UUID owner, World world, List<Coordinate> coordinates,
                         Consumer<ChunkAnalysisResult> callback, Runnable complete) {
            this.owner = owner;
            this.world = world;
            this.coordinates = coordinates;
            this.callback = callback;
            this.complete = complete;
        }

        @Override
        public void run() {
            PluginConfig.Monitoring budget = config.get().monitoring();
            long deadline = System.nanoTime() + (long) (budget.maxMillisecondsPerTick() * 1_000_000.0);
            int submitted = 0;
            while (index < coordinates.size()
                    && submitted < budget.chunksPerCycle()
                    && System.nanoTime() < deadline) {
                Coordinate coordinate = coordinates.get(index++);
                if (!world.isChunkLoaded(coordinate.x(), coordinate.z())) {
                    continue;
                }
                Chunk chunk = world.getChunkAt(coordinate.x(), coordinate.z());
                if (!pipeline.submit(collector.collectQuick(chunk), callback)) {
                    index--;
                    return;
                }
                submitted++;
            }
            if (index >= coordinates.size()) {
                cancel();
                active.remove(owner);
                complete.run();
            }
        }
    }

    public record StartResult(boolean accepted, int loadedChunks) {
    }

    private record Coordinate(int x, int z) {
    }
}
