package dev.chunkdoctor.analysis;

import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkSnapshot;
import dev.chunkdoctor.model.Confidence;
import dev.chunkdoctor.recommendation.RecommendationEngine;
import dev.chunkdoctor.scoring.RiskCalculation;
import dev.chunkdoctor.scoring.RiskCalculator;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ChunkAnalyzer {
    private final RiskCalculator calculator;
    private final RecommendationEngine recommendationEngine;

    public ChunkAnalyzer(RiskCalculator calculator, RecommendationEngine recommendationEngine) {
        this.calculator = calculator;
        this.recommendationEngine = recommendationEngine;
    }

    public ChunkAnalysisResult analyze(ChunkSnapshot snapshot) {
        RiskCalculation calculation = calculator.calculate(snapshot);
        return new ChunkAnalysisResult(snapshot.key(), Instant.now(), calculation.score(), calculation.level(),
                confidence(snapshot), metrics(snapshot), calculation.reasons(),
                recommendationEngine.recommendationsFor(snapshot), snapshot.deepScan());
    }

    private static Confidence confidence(ChunkSnapshot snapshot) {
        if (snapshot.deepScan()) {
            return Confidence.HIGH;
        }
        if (snapshot.entityDataComplete() && snapshot.nearbyPlayers() > 0) {
            return Confidence.MEDIUM;
        }
        return Confidence.LOW;
    }

    private static Map<String, Integer> metrics(ChunkSnapshot s) {
        Map<String, Integer> values = new LinkedHashMap<>();
        values.put("entities", s.entities().total());
        values.put("itemEntities", s.entities().itemEntities());
        values.put("villagers", s.entities().villagers());
        values.put("aiMobs", s.entities().aiMobs());
        values.put("minecarts", s.entities().minecarts());
        values.put("boats", s.entities().boats());
        values.put("itemFrames", s.entities().itemFrames());
        values.put("hoppers", s.blocks().hoppers());
        values.put("activeHoppers", s.blocks().activeHoppers());
        values.put("furnaces", s.blocks().furnaces());
        values.put("activeFurnaces", s.blocks().activeFurnaces());
        values.put("pistons", s.blocks().pistons());
        values.put("observers", s.blocks().observers());
        values.put("repeaters", s.blocks().repeaters());
        values.put("comparators", s.blocks().comparators());
        values.put("redstone", s.blocks().redstone());
        values.put("activeRedstoneComponents", s.blocks().activeRedstoneComponents());
        values.put("spawners", s.blocks().spawners());
        values.put("containers", s.blocks().containers());
        values.put("blockEntities", s.blocks().blockEntities());
        values.put("longestHopperLine", s.blocks().longestHopperLine());
        values.put("nearbyPlayers", s.nearbyPlayers());
        values.put("scannedBlocks", s.scannedBlocks());
        return values;
    }
}
