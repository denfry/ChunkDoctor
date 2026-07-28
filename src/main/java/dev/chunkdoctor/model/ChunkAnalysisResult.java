package dev.chunkdoctor.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record ChunkAnalysisResult(
        ChunkKey key,
        Instant analyzedAt,
        int riskScore,
        RiskLevel riskLevel,
        Confidence confidence,
        Map<String, Integer> metrics,
        List<AnalysisReason> reasons,
        List<String> recommendations,
        boolean deepScan
) {
    public ChunkAnalysisResult {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(analyzedAt, "analyzedAt");
        Objects.requireNonNull(riskLevel, "riskLevel");
        Objects.requireNonNull(confidence, "confidence");
        metrics = Map.copyOf(metrics);
        reasons = List.copyOf(reasons);
        recommendations = List.copyOf(recommendations);
        if (riskScore < 0 || riskScore > 100) {
            throw new IllegalArgumentException("riskScore must be in [0, 100]");
        }
    }
}
