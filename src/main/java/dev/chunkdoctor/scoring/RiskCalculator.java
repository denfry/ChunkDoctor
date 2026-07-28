package dev.chunkdoctor.scoring;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.AnalysisReason;
import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.ChunkSnapshot;
import dev.chunkdoctor.model.EntityMetrics;
import dev.chunkdoctor.model.RiskLevel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

public final class RiskCalculator {
    private final Supplier<PluginConfig> config;

    public RiskCalculator(Supplier<PluginConfig> config) {
        this.config = config;
    }

    public RiskCalculation calculate(ChunkSnapshot snapshot) {
        PluginConfig.Risk risk = config.get().risk();
        RiskWeights weights = risk.weights();
        EntityMetrics e = snapshot.entities();
        BlockMetrics b = snapshot.blocks();
        List<AnalysisReason> reasons = new ArrayList<>();

        double score = 0.0;
        score += add(reasons, "entities", e.total(), weights.entities(), "Сущности");
        score += add(reasons, "items", e.itemEntities(), weights.itemEntities(), "Предметы на земле");
        score += add(reasons, "villagers", e.villagers(), weights.villagers(), "Жители");
        score += add(reasons, "ai-mobs", e.aiMobs(), weights.aiMobs(), "Мобы с активным AI");
        score += add(reasons, "minecarts", e.minecarts(), weights.minecarts(), "Вагонетки");
        score += add(reasons, "boats", e.boats(), weights.boats(), "Лодки");
        score += add(reasons, "item-frames", e.itemFrames(), weights.itemFrames(), "Рамки");
        score += add(reasons, "hoppers", b.hoppers(), weights.hoppers(), "Воронки");
        score += add(reasons, "active-hoppers", b.activeHoppers(), weights.activeHoppers(), "Активные воронки");
        score += add(reasons, "furnaces", b.furnaces(), weights.furnaces(), "Печи");
        score += add(reasons, "active-furnaces", b.activeFurnaces(), weights.activeFurnaces(), "Активные печи");
        score += add(reasons, "pistons", b.pistons(), weights.pistons(), "Поршни");
        score += add(reasons, "observers", b.observers(), weights.observers(), "Наблюдатели");
        score += add(reasons, "repeaters", b.repeaters(), weights.repeaters(), "Повторители");
        score += add(reasons, "comparators", b.comparators(), weights.comparators(), "Компараторы");
        score += add(reasons, "redstone", b.redstone(), weights.redstone(), "Редстоун");
        score += add(reasons, "active-redstone", b.activeRedstoneComponents(),
                weights.activeRedstone(), "Запитанные редстоун-компоненты");
        score += add(reasons, "spawners", b.spawners(), weights.spawners(), "Спавнеры");
        score += add(reasons, "containers", b.containers(), weights.containers(), "Контейнеры");
        score += add(reasons, "block-entities", b.blockEntities(), weights.blockEntities(), "Блочные сущности");

        PluginConfig.ExcessThresholds x = risk.excess();
        score += excessPenalty(e.total(), x.entities(), risk.thresholdPenaltyFactor());
        score += excessPenalty(e.itemEntities(), x.items(), risk.thresholdPenaltyFactor());
        score += excessPenalty(e.villagers(), x.villagers(), risk.thresholdPenaltyFactor());
        score += excessPenalty(e.minecarts(), x.minecarts(), risk.thresholdPenaltyFactor());
        score += excessPenalty(b.hoppers(), x.hoppers(), risk.thresholdPenaltyFactor());
        score += excessPenalty(b.blockEntities(), x.blockEntities(), risk.thresholdPenaltyFactor());
        score += excessPenalty(b.redstoneComponents(), x.redstoneComponents(), risk.thresholdPenaltyFactor());

        double densityRatio = Math.max(
                ratio(e.total(), x.entities()),
                Math.max(ratio(b.blockEntities(), x.blockEntities()), ratio(b.redstoneComponents(), x.redstoneComponents())));
        if (densityRatio > 1.0) {
            double densityPenalty = Math.pow(densityRatio - 1.0, 1.35) * risk.densityPenaltyFactor();
            score += densityPenalty;
            reasons.add(new AnalysisReason("density", 0, densityPenalty, "Высокая плотность объектов"));
        }

        int finalScore = (int) Math.round(Math.max(0.0, Math.min(100.0, score)));
        reasons.removeIf(reason -> reason.contribution() < 1.0);
        reasons.sort(Comparator.comparingDouble(AnalysisReason::contribution).reversed()
                .thenComparing(AnalysisReason::key));
        return new RiskCalculation(finalScore, levelFor(finalScore, risk), reasons.stream().limit(5).toList());
    }

    public static RiskLevel levelFor(int score, PluginConfig.Risk risk) {
        if (score >= risk.criticalThreshold()) {
            return RiskLevel.CRITICAL;
        }
        if (score >= risk.highThreshold()) {
            return RiskLevel.HIGH;
        }
        if (score >= risk.mediumThreshold()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public static double excessPenalty(int count, int threshold, double factor) {
        if (count <= threshold) {
            return 0.0;
        }
        double excess = count - threshold;
        return factor * excess * excess / threshold;
    }

    private static double add(List<AnalysisReason> reasons, String key, int count, double weight, String label) {
        double contribution = count * weight;
        if (count > 0 && contribution >= 1.0) {
            reasons.add(new AnalysisReason(key, count, contribution, label + ": " + count));
        }
        return contribution;
    }

    private static double ratio(int count, int threshold) {
        return count / (double) threshold;
    }
}
