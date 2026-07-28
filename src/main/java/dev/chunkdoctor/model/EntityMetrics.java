package dev.chunkdoctor.model;

public record EntityMetrics(
        int total,
        int itemEntities,
        int villagers,
        int aiMobs,
        int minecarts,
        int boats,
        int itemFrames
) {
    public static EntityMetrics empty() {
        return new EntityMetrics(0, 0, 0, 0, 0, 0, 0);
    }
}
