package dev.chunkdoctor.analysis;

import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.ChunkSnapshot;
import dev.chunkdoctor.model.EntityMetrics;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;

import java.time.Instant;

/**
 * Collects Bukkit state synchronously and returns a Bukkit-free snapshot.
 */
public final class ChunkSnapshotCollector {
    public ChunkSnapshot collectQuick(Chunk chunk) {
        requirePrimaryThread();
        if (!chunk.isLoaded()) {
            throw new IllegalStateException("Chunk unloaded before collection");
        }

        boolean entitiesLoaded = chunk.isEntitiesLoaded();
        EntityMetrics entities = entitiesLoaded ? collectEntities(chunk.getEntities()) : EntityMetrics.empty();
        BlockMetrics blocks = collectTileEntities(chunk.getTileEntities(false));
        int nearbyPlayers = 0;
        for (Player player : chunk.getWorld().getPlayers()) {
            if (Math.abs(player.getLocation().getChunk().getX() - chunk.getX()) <= 1
                    && Math.abs(player.getLocation().getChunk().getZ() - chunk.getZ()) <= 1) {
                nearbyPlayers++;
            }
        }
        return new ChunkSnapshot(
                new ChunkKey(chunk.getWorld().getUID(), chunk.getWorld().getName(), chunk.getX(), chunk.getZ()),
                Instant.now(), entities, blocks, true, entitiesLoaded, nearbyPlayers, false, 0);
    }

    public EntityMetrics collectEntities(Entity[] entities) {
        int items = 0;
        int villagers = 0;
        int aiMobs = 0;
        int minecarts = 0;
        int boats = 0;
        int itemFrames = 0;
        for (Entity entity : entities) {
            if (entity instanceof Item) {
                items++;
            }
            if (entity instanceof Villager) {
                villagers++;
            }
            if (entity instanceof Mob mob && mob.hasAI()) {
                aiMobs++;
            }
            if (entity instanceof Minecart) {
                minecarts++;
            }
            if (entity instanceof Boat) {
                boats++;
            }
            if (entity instanceof ItemFrame) {
                itemFrames++;
            }
        }
        return new EntityMetrics(entities.length, items, villagers, aiMobs, minecarts, boats, itemFrames);
    }

    private static BlockMetrics collectTileEntities(BlockState[] states) {
        int hoppers = 0;
        int activeHoppers = 0;
        int furnaces = 0;
        int activeFurnaces = 0;
        int spawners = 0;
        int containers = 0;
        for (BlockState state : states) {
            if (state instanceof Hopper hopper) {
                hoppers++;
                if (!hopper.getBlock().isBlockIndirectlyPowered()) {
                    activeHoppers++;
                }
            }
            if (state instanceof Furnace furnace) {
                furnaces++;
                if (furnace.getBurnTime() > 0) {
                    activeFurnaces++;
                }
            }
            if (state instanceof CreatureSpawner) {
                spawners++;
            }
            if (state instanceof Container) {
                containers++;
            }
        }
        return new BlockMetrics(hoppers, activeHoppers, furnaces, activeFurnaces,
                0, 0, 0, 0, 0, 0, spawners, containers, states.length, 0);
    }

    private static void requirePrimaryThread() {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Bukkit chunk snapshots must be collected on the primary thread");
        }
    }
}
