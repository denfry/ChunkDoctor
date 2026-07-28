package dev.chunkdoctor.analysis;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.ChunkSnapshot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Hopper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Manual-only full block scan split across ticks with both block and time budgets.
 */
public final class DeepScanService {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> config;
    private final ChunkSnapshotCollector collector;
    private final AnalysisPipeline pipeline;
    private final Map<ChunkKey, BukkitRunnable> active = new HashMap<>();

    public DeepScanService(JavaPlugin plugin, Supplier<PluginConfig> config,
                           ChunkSnapshotCollector collector, AnalysisPipeline pipeline) {
        this.plugin = plugin;
        this.config = config;
        this.collector = collector;
        this.pipeline = pipeline;
    }

    public boolean start(Player player, Consumer<dev.chunkdoctor.model.ChunkAnalysisResult> callback) {
        PluginConfig.DeepScan settings = config.get().deepScan();
        if (!settings.enabled() || active.size() >= settings.maximumConcurrentScans()) {
            return false;
        }
        Chunk chunk = player.getLocation().getChunk();
        ChunkKey key = new ChunkKey(chunk.getWorld().getUID(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ());
        if (active.containsKey(key)) {
            return false;
        }
        ChunkSnapshot base = collector.collectQuick(chunk);
        DeepTask task = new DeepTask(player, chunk, base, callback);
        active.put(key, task);
        task.runTaskTimer(plugin, 1L, 1L);
        return true;
    }

    public void cancelWorld(World world) {
        active.entrySet().removeIf(entry -> {
            if (entry.getKey().worldId().equals(world.getUID())) {
                entry.getValue().cancel();
                return true;
            }
            return false;
        });
    }

    public void cancelChunk(UUID worldId, int chunkX, int chunkZ) {
        active.entrySet().removeIf(entry -> {
            ChunkKey key = entry.getKey();
            if (key.worldId().equals(worldId) && key.chunkX() == chunkX && key.chunkZ() == chunkZ) {
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

    public int activeCount() {
        return active.size();
    }

    private final class DeepTask extends BukkitRunnable {
        private final CommandSender initiator;
        private final Chunk chunk;
        private final ChunkSnapshot base;
        private final Consumer<dev.chunkdoctor.model.ChunkAnalysisResult> callback;
        private final long started = System.nanoTime();
        private final int minY;
        private final int height;
        private final int totalBlocks;
        private final MutableBlockCounts counts = new MutableBlockCounts();
        private int index;
        private int ticks;

        private DeepTask(CommandSender initiator, Chunk chunk, ChunkSnapshot base,
                         Consumer<dev.chunkdoctor.model.ChunkAnalysisResult> callback) {
            this.initiator = initiator;
            this.chunk = chunk;
            this.base = base;
            this.callback = callback;
            this.minY = chunk.getWorld().getMinHeight();
            this.height = chunk.getWorld().getMaxHeight() - minY;
            this.totalBlocks = 16 * 16 * height;
        }

        @Override
        public void run() {
            PluginConfig.DeepScan settings = config.get().deepScan();
            if (!plugin.isEnabled() || !chunk.isLoaded()) {
                finishCancelled("Глубокое сканирование отменено: чанк был выгружен.");
                return;
            }
            if (System.nanoTime() - started > settings.maximumDurationSeconds() * 1_000_000_000L) {
                finishCancelled("Глубокое сканирование остановлено по лимиту времени; неполный результат не сохранён.");
                return;
            }
            long deadline = System.nanoTime() + (long) (settings.maximumMillisecondsPerTick() * 1_000_000.0);
            int processed = 0;
            while (index < totalBlocks && processed < settings.blocksPerTick() && System.nanoTime() < deadline) {
                int yIndex = index / 256;
                int horizontal = index % 256;
                int localZ = horizontal / 16;
                int localX = horizontal % 16;
                Block block = chunk.getBlock(localX, minY + yIndex, localZ);
                counts.accept(block, localX, minY + yIndex, localZ);
                index++;
                processed++;
            }
            ticks++;
            if (ticks % 20 == 0 && initiator instanceof Player player && player.isOnline()) {
                int percent = (int) ((index * 100L) / totalBlocks);
                player.sendActionBar(Component.text("ChunkDoctor deep: " + percent + "%", NamedTextColor.GOLD));
            }
            if (index >= totalBlocks) {
                complete();
            }
        }

        private void complete() {
            cancel();
            active.remove(base.key());
            BlockMetrics blocks = counts.toMetrics(base.blocks().blockEntities());
            ChunkSnapshot snapshot = new ChunkSnapshot(base.key(), Instant.now(), base.entities(), blocks,
                    true, base.entityDataComplete(), base.nearbyPlayers(), true, totalBlocks);
            if (!pipeline.submit(snapshot, callback)) {
                initiator.sendMessage(Component.text("Очередь анализа заполнена; повторите позже.", NamedTextColor.RED));
            }
        }

        private void finishCancelled(String message) {
            cancel();
            active.remove(base.key());
            initiator.sendMessage(Component.text(message, NamedTextColor.YELLOW));
        }
    }

    private static final class MutableBlockCounts {
        private static final Set<Material> CONTAINERS = Set.of(
                Material.CHEST, Material.TRAPPED_CHEST, Material.BARREL, Material.DISPENSER,
                Material.DROPPER, Material.HOPPER, Material.FURNACE, Material.BLAST_FURNACE,
                Material.SMOKER, Material.BREWING_STAND, Material.CHISELED_BOOKSHELF);
        private final Set<Long> hoppers = new HashSet<>();
        private int activeHoppers;
        private int furnaces;
        private int activeFurnaces;
        private int pistons;
        private int observers;
        private int repeaters;
        private int comparators;
        private int redstone;
        private int activeRedstone;
        private int spawners;
        private int containers;

        void accept(Block block, int x, int y, int z) {
            Material type = block.getType();
            if (CONTAINERS.contains(type) || type.name().endsWith("_SHULKER_BOX")) {
                containers++;
            }
            switch (type) {
                case HOPPER -> {
                    hoppers.add(pack(x, y, z));
                    if (block.getBlockData() instanceof Hopper hopper && hopper.isEnabled()) {
                        activeHoppers++;
                    }
                }
                case FURNACE, BLAST_FURNACE, SMOKER -> {
                    furnaces++;
                    if (block.getBlockData() instanceof Lightable lightable && lightable.isLit()) {
                        activeFurnaces++;
                    }
                }
                case PISTON, STICKY_PISTON, MOVING_PISTON -> pistons++;
                case OBSERVER -> observers++;
                case REPEATER -> repeaters++;
                case COMPARATOR -> comparators++;
                case REDSTONE_WIRE, REDSTONE_TORCH, REDSTONE_WALL_TORCH -> redstone++;
                case SPAWNER, TRIAL_SPAWNER -> spawners++;
                default -> {
                }
            }
            if (isRedstoneComponent(type)
                    && (block.isBlockPowered() || block.isBlockIndirectlyPowered())) {
                activeRedstone++;
            }
        }

        BlockMetrics toMetrics(int blockEntities) {
            return new BlockMetrics(hoppers.size(), activeHoppers, furnaces, activeFurnaces,
                    pistons, observers, repeaters, comparators, redstone, activeRedstone, spawners,
                    containers, blockEntities, longestHopperLine());
        }

        private static boolean isRedstoneComponent(Material type) {
            return switch (type) {
                case PISTON, STICKY_PISTON, MOVING_PISTON, OBSERVER, REPEATER, COMPARATOR,
                        REDSTONE_WIRE, REDSTONE_TORCH, REDSTONE_WALL_TORCH -> true;
                default -> false;
            };
        }

        private int longestHopperLine() {
            int longest = 0;
            for (long packed : hoppers) {
                int x = unpackX(packed);
                int y = unpackY(packed);
                int z = unpackZ(packed);
                if (!hoppers.contains(pack(x - 1, y, z))) {
                    int length = 1;
                    while (hoppers.contains(pack(x + length, y, z))) {
                        length++;
                    }
                    longest = Math.max(longest, length);
                }
                if (!hoppers.contains(pack(x, y, z - 1))) {
                    int length = 1;
                    while (hoppers.contains(pack(x, y, z + length))) {
                        length++;
                    }
                    longest = Math.max(longest, length);
                }
            }
            return longest;
        }

        private static long pack(int x, int y, int z) {
            return ((long) (y + 2_048) << 8) | ((long) (z & 15) << 4) | (x & 15);
        }

        private static int unpackX(long value) {
            return (int) (value & 15);
        }

        private static int unpackZ(long value) {
            return (int) ((value >>> 4) & 15);
        }

        private static int unpackY(long value) {
            return (int) (value >>> 8) - 2_048;
        }
    }
}
