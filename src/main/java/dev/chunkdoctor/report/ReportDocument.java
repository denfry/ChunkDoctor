package dev.chunkdoctor.report;

import dev.chunkdoctor.model.ChunkAnalysisResult;

import java.time.Instant;
import java.util.List;

public record ReportDocument(
        Instant generatedAt,
        String pluginVersion,
        String serverVersion,
        Summary summary,
        List<ChunkAnalysisResult> chunks
) {
    public ReportDocument {
        chunks = List.copyOf(chunks);
    }

    public record Summary(int analyzedChunks, int lowRiskChunks, int mediumRiskChunks,
                          int highRiskChunks, int criticalChunks) {
    }
}
