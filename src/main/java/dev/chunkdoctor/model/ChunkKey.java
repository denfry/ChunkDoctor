package dev.chunkdoctor.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable chunk identity. World names are display metadata and do not participate
 * in equality, so renaming a world does not duplicate cached results.
 */
public record ChunkKey(UUID worldId, String worldName, int chunkX, int chunkZ) {
    public ChunkKey {
        Objects.requireNonNull(worldId, "worldId");
        worldName = Objects.requireNonNull(worldName, "worldName");
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ChunkKey key
                && worldId.equals(key.worldId)
                && chunkX == key.chunkX
                && chunkZ == key.chunkZ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldId, chunkX, chunkZ);
    }

    public String display() {
        return worldName + " [" + chunkX + ", " + chunkZ + "]";
    }
}
