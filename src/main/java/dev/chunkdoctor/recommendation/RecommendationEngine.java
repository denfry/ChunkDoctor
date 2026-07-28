package dev.chunkdoctor.recommendation;

import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.ChunkSnapshot;
import dev.chunkdoctor.model.EntityMetrics;

import java.util.ArrayList;
import java.util.List;

public final class RecommendationEngine {
    public List<String> recommendationsFor(ChunkSnapshot snapshot) {
        EntityMetrics e = snapshot.entities();
        BlockMetrics b = snapshot.blocks();
        List<String> recommendations = new ArrayList<>();
        if (b.hoppers() >= 32 || b.longestHopperLine() >= 12) {
            recommendations.add("Замените длинные линии воронок водными потоками или пакетной транспортировкой.");
        }
        if (e.villagers() >= 24) {
            recommendations.add("Разделите жителей на несколько зон и сократите активные рабочие места.");
        }
        if (e.itemEntities() >= 64) {
            recommendations.add("Добавьте сбор или безопасное удаление лишних предметов.");
        }
        if (e.minecarts() >= 20) {
            recommendations.add("Сократите количество постоянно загруженных вагонеток.");
        }
        if (b.redstoneComponents() >= 96 || b.activeRedstoneComponents() >= 32) {
            recommendations.add("Проверьте зацикленный редстоун и отключайте механизм, когда он не нужен.");
        }
        if (e.aiMobs() >= 64) {
            recommendations.add("Ограничьте размножение мобов и распределите ферму между зонами.");
        }
        if (b.activeFurnaces() >= 24) {
            recommendations.add("Распределите обработку печей по времени вместо одновременной работы.");
        }
        if (b.spawners() >= 4) {
            recommendations.add("Проверьте накопление мобов вокруг спавнеров и пределы фермы.");
        }
        return List.copyOf(recommendations);
    }
}
