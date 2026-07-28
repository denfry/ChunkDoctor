package dev.chunkdoctor;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.model.BlockMetrics;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.ChunkSnapshot;
import dev.chunkdoctor.model.EntityMetrics;
import dev.chunkdoctor.scoring.RiskWeights;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class TestFixtures {
    public static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private TestFixtures() {
    }

    public static PluginConfig config() {
        RiskWeights weights = new RiskWeights(0.2, 0.4, 2.5, 0.8, 1.5, 0.6, 0.35,
                1.2, 0.4, 0.7, 0.8, 1.0, 1.0, 0.65, 0.75, 0.2, 0.5, 4.0, 0.35, 0.15);
        PluginConfig.Risk risk = new PluginConfig.Risk(30, 60, 80, weights,
                new PluginConfig.ExcessThresholds(100, 64, 24, 20, 32, 64, 96),
                0.075, 8.0);
        return new PluginConfig(
                new PluginConfig.Monitoring(true, 100, 2, 2.0, 300_000, 3_600_000,
                        17, 18.5, 2, 64),
                new PluginConfig.Worlds(false, Set.of()),
                new PluginConfig.ManualScan(8, 128),
                new PluginConfig.DeepScan(true, 2_048, 3.0, 30, 1),
                risk,
                new PluginConfig.Notifications(true, true, 15, 300_000),
                new PluginConfig.Export("reports", true),
                Map.of());
    }

    public static ChunkSnapshot snapshot(EntityMetrics entities, BlockMetrics blocks, boolean deep) {
        return new ChunkSnapshot(new ChunkKey(WORLD_ID, "world", 5, -2), Instant.parse("2026-07-28T12:00:00Z"),
                entities, blocks, true, true, 1, deep, deep ? 98_304 : 0);
    }
}
