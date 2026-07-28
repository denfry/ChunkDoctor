package dev.chunkdoctor.monitoring;

import dev.chunkdoctor.TestFixtures;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.Confidence;
import dev.chunkdoctor.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultRepositoryTest {
    @Test
    void rankedSortsByScoreThenStableCoordinates() {
        ChunkAnalysisResult low = result("b", 4, 2, 25);
        ChunkAnalysisResult highLater = result("z", 2, 0, 80);
        ChunkAnalysisResult highFirst = result("a", 1, 0, 80);
        List<ChunkAnalysisResult> values = List.of(low, highLater, highFirst).stream()
                .sorted(ResultRepository.rankingComparator()).toList();

        assertEquals(List.of(highFirst, highLater, low), values);
    }

    private ChunkAnalysisResult result(String world, int x, int z, int score) {
        return new ChunkAnalysisResult(
                new ChunkKey(UUID.nameUUIDFromBytes(world.getBytes()), world, x, z),
                Instant.parse("2026-07-28T12:00:00Z"), score,
                score >= 80 ? RiskLevel.CRITICAL : RiskLevel.LOW,
                Confidence.MEDIUM, Map.of(), List.of(), List.of(), false);
    }
}
