package dev.chunkdoctor.recommendation;

import dev.chunkdoctor.TestFixtures;
import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.EntityMetrics;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecommendationEngineTest {
    private final RecommendationEngine engine = new RecommendationEngine();

    @Test
    void recommendationsOnlyMatchObservedProblems() {
        EntityMetrics entities = new EntityMetrics(80, 70, 0, 0, 0, 0, 0);
        BlockMetrics blocks = new BlockMetrics(40, 40, 0, 0, 0, 0,
                0, 0, 0, 0, 0, 40, 40, 15);

        List<String> recommendations = engine.recommendationsFor(TestFixtures.snapshot(entities, blocks, true));

        assertEquals(2, recommendations.size());
        assertTrue(recommendations.stream().anyMatch(text -> text.contains("воронок")));
        assertTrue(recommendations.stream().anyMatch(text -> text.contains("предметов")));
    }

    @Test
    void healthyChunkGetsNoGenericAdvice() {
        assertTrue(engine.recommendationsFor(
                TestFixtures.snapshot(EntityMetrics.empty(), BlockMetrics.empty(), false)).isEmpty());
    }
}
