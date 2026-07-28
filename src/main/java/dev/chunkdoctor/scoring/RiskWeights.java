package dev.chunkdoctor.scoring;

public record RiskWeights(
        double entities,
        double itemEntities,
        double villagers,
        double aiMobs,
        double minecarts,
        double boats,
        double itemFrames,
        double hoppers,
        double activeHoppers,
        double furnaces,
        double activeFurnaces,
        double pistons,
        double observers,
        double repeaters,
        double comparators,
        double redstone,
        double activeRedstone,
        double spawners,
        double containers,
        double blockEntities
) {
}
