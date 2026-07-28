package dev.chunkdoctor.model;

public record BlockMetrics(
        int hoppers,
        int activeHoppers,
        int furnaces,
        int activeFurnaces,
        int pistons,
        int observers,
        int repeaters,
        int comparators,
        int redstone,
        int activeRedstoneComponents,
        int spawners,
        int containers,
        int blockEntities,
        int longestHopperLine
) {
    public static BlockMetrics empty() {
        return new BlockMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    public int redstoneComponents() {
        return pistons + observers + repeaters + comparators + redstone;
    }
}
