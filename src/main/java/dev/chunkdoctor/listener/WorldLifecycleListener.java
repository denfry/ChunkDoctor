package dev.chunkdoctor.listener;

import dev.chunkdoctor.analysis.DeepScanService;
import dev.chunkdoctor.analysis.ManualScanService;
import dev.chunkdoctor.monitoring.ResultRepository;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldUnloadEvent;

public final class WorldLifecycleListener implements Listener {
    private final DeepScanService deepScans;
    private final ManualScanService manualScans;
    private final ResultRepository repository;

    public WorldLifecycleListener(DeepScanService deepScans, ManualScanService manualScans,
                                  ResultRepository repository) {
        this.deepScans = deepScans;
        this.manualScans = manualScans;
        this.repository = repository;
    }

    @EventHandler
    public void onWorldUnload(WorldUnloadEvent event) {
        deepScans.cancelWorld(event.getWorld());
        manualScans.cancelWorld(event.getWorld());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        deepScans.cancelChunk(event.getWorld().getUID(), event.getChunk().getX(), event.getChunk().getZ());
        repository.remove(new dev.chunkdoctor.model.ChunkKey(event.getWorld().getUID(),
                event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ()));
    }
}
