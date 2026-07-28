package dev.chunkdoctor.scoring;

import dev.chunkdoctor.TestFixtures;
import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.EntityMetrics;
import dev.chunkdoctor.model.RiskLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskCalculatorTest {
    private final RiskCalculator calculator = new RiskCalculator(TestFixtures::config);

    @Test
    void emptyChunkHasNoRisk() {
        RiskCalculation calculation = calculator.calculate(
                TestFixtures.snapshot(EntityMetrics.empty(), BlockMetrics.empty(), false));

        assertEquals(0, calculation.score());
        assertEquals(RiskLevel.LOW, calculation.level());
        assertTrue(calculation.reasons().isEmpty());
    }

    @Test
    void denseFarmIsClampedAndCritical() {
        EntityMetrics entities = new EntityMetrics(250, 100, 70, 100, 40, 0, 0);
        BlockMetrics blocks = new BlockMetrics(220, 200, 30, 25, 40, 30,
                30, 20, 150, 80, 5, 230, 250, 30);

        RiskCalculation calculation = calculator.calculate(TestFixtures.snapshot(entities, blocks, true));

        assertEquals(100, calculation.score());
        assertEquals(RiskLevel.CRITICAL, calculation.level());
        assertTrue(calculation.reasons().stream().anyMatch(reason -> reason.key().equals("hoppers")));
    }

    @Test
    void densityReasonAppearsWhenItIsAmongTopCauses() {
        EntityMetrics entities = new EntityMetrics(180, 0, 0, 0, 0, 0, 0);
        RiskCalculation calculation = calculator.calculate(
                TestFixtures.snapshot(entities, BlockMetrics.empty(), false));

        assertTrue(calculation.reasons().stream().anyMatch(reason -> reason.key().equals("density")));
    }

    @Test
    void levelsRespectConfiguredBoundaries() {
        var risk = TestFixtures.config().risk();
        assertEquals(RiskLevel.LOW, RiskCalculator.levelFor(29, risk));
        assertEquals(RiskLevel.MEDIUM, RiskCalculator.levelFor(30, risk));
        assertEquals(RiskLevel.HIGH, RiskCalculator.levelFor(60, risk));
        assertEquals(RiskLevel.CRITICAL, RiskCalculator.levelFor(80, risk));
    }

    @Test
    void excessPenaltyIsNonlinearAndStartsAfterThreshold() {
        assertEquals(0.0, RiskCalculator.excessPenalty(100, 100, 0.075));
        double first = RiskCalculator.excessPenalty(120, 100, 0.075);
        double second = RiskCalculator.excessPenalty(140, 100, 0.075);
        assertTrue(second > first * 3.9);
    }
}
