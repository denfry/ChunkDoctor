package dev.chunkdoctor.model;

import java.util.Objects;

public record AnalysisReason(String key, int count, double contribution, String message) {
    public AnalysisReason {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(message, "message");
    }
}
