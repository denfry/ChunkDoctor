package dev.chunkdoctor.monitoring;

import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkKey;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class ResultRepository {
    private static final Comparator<ChunkAnalysisResult> RANKING =
            Comparator.comparingInt(ChunkAnalysisResult::riskScore).reversed()
                    .thenComparing(result -> result.key().worldName(), String.CASE_INSENSITIVE_ORDER)
                    .thenComparingInt(result -> result.key().chunkX())
                    .thenComparingInt(result -> result.key().chunkZ());

    private final ConcurrentMap<ChunkKey, ChunkAnalysisResult> results = new ConcurrentHashMap<>();

    public ChunkAnalysisResult put(ChunkAnalysisResult result) {
        return results.put(result.key(), result);
    }

    public Optional<ChunkAnalysisResult> get(ChunkKey key) {
        return Optional.ofNullable(results.get(key));
    }

    public List<ChunkAnalysisResult> ranked() {
        List<ChunkAnalysisResult> copy = new ArrayList<>(results.values());
        copy.sort(RANKING);
        return List.copyOf(copy);
    }

    public int removeExpired(long expirationMillis) {
        Instant cutoff = Instant.now().minusMillis(expirationMillis);
        int before = results.size();
        results.entrySet().removeIf(entry -> entry.getValue().analyzedAt().isBefore(cutoff));
        return before - results.size();
    }

    public void remove(ChunkKey key) {
        results.remove(key);
    }

    public int size() {
        return results.size();
    }

    public void clear() {
        results.clear();
    }

    public static Comparator<ChunkAnalysisResult> rankingComparator() {
        return RANKING;
    }
}
