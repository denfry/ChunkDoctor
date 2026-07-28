package dev.chunkdoctor.scoring;

import dev.chunkdoctor.model.AnalysisReason;
import dev.chunkdoctor.model.RiskLevel;

import java.util.List;

public record RiskCalculation(int score, RiskLevel level, List<AnalysisReason> reasons) {
    public RiskCalculation {
        reasons = List.copyOf(reasons);
    }
}
