package dev.chunkdoctor.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable, Bukkit-free payload safe to process on worker threads.
 */
public record ChunkSnapshot(
        ChunkKey key,
        Instant collectedAt,
        EntityMetrics entities,
        BlockMetrics blocks,
        boolean loadedAtCollection,
        boolean entityDataComplete,
        int nearbyPlayers,
        boolean deepScan,
        int scannedBlocks
) {
    public ChunkSnapshot {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(collectedAt, "collectedAt");
        Objects.requireNonNull(entities, "entities");
        Objects.requireNonNull(blocks, "blocks");
    }
}
