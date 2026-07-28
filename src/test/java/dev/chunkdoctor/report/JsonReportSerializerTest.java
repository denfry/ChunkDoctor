package dev.chunkdoctor.report;

import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.Confidence;
import dev.chunkdoctor.model.RiskLevel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonReportSerializerTest {
    @Test
    void serializesStableIsoTimestampAndChunkMetrics() {
        ChunkAnalysisResult result = new ChunkAnalysisResult(
                new ChunkKey(UUID.fromString("00000000-0000-0000-0000-000000000001"), "world", 2, -3),
                Instant.parse("2026-07-28T12:00:00Z"), 84, RiskLevel.CRITICAL,
                Confidence.HIGH, Map.of("hoppers", 218), List.of(), List.of("Разделите ферму"), true);
        ReportDocument document = new ReportDocument(Instant.parse("2026-07-28T12:01:00Z"),
                "1.0.0", "Paper 1.21.8",
                new ReportDocument.Summary(1, 0, 0, 0, 1), List.of(result));

        String json = new JsonReportSerializer(true).serialize(document);

        assertTrue(json.contains("\"generatedAt\": \"2026-07-28T12:01:00Z\""));
        assertTrue(json.contains("\"hoppers\": 218"));
        assertTrue(json.contains("\"riskLevel\": \"CRITICAL\""));
    }
}
