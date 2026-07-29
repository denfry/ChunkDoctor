package dev.chunkdoctor;

import dev.chunkdoctor.analysis.AnalysisPipeline;
import dev.chunkdoctor.analysis.ChunkAnalyzer;
import dev.chunkdoctor.analysis.ChunkSnapshotCollector;
import dev.chunkdoctor.analysis.DeepScanService;
import dev.chunkdoctor.analysis.ManualScanService;
import dev.chunkdoctor.command.ChunkDoctorCommand;
import dev.chunkdoctor.config.ConfigLoader;
import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.listener.WorldLifecycleListener;
import dev.chunkdoctor.message.MessageService;
import dev.chunkdoctor.metrics.MetricsBootstrap;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.monitoring.MonitoringService;
import dev.chunkdoctor.monitoring.ResultRepository;
import dev.chunkdoctor.notification.NotificationService;
import dev.chunkdoctor.recommendation.RecommendationEngine;
import dev.chunkdoctor.report.ReportService;
import dev.chunkdoctor.scoring.RiskCalculator;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ChunkDoctorPlugin extends JavaPlugin {
    private final AtomicReference<PluginConfig> configRef = new AtomicReference<>();
    private MonitoringService monitoring;
    private DeepScanService deepScans;
    private ManualScanService manualScans;
    private AnalysisPipeline pipeline;
    private ReportService reports;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadValidatedConfig();

        MessageService messages = new MessageService(configRef::get);
        ResultRepository repository = new ResultRepository();
        ChunkSnapshotCollector collector = new ChunkSnapshotCollector();
        RiskCalculator calculator = new RiskCalculator(configRef::get);
        ChunkAnalyzer analyzer = new ChunkAnalyzer(calculator, new RecommendationEngine());
        NotificationService notifications = new NotificationService(this, configRef::get, messages);
        PluginConfig.Monitoring workerConfig = configRef.get().monitoring();
        pipeline = new AnalysisPipeline(this, analyzer, result -> storeResult(repository, notifications, result),
                workerConfig.workerThreads(), workerConfig.maximumPendingAnalyses());
        monitoring = new MonitoringService(this, configRef::get, collector, pipeline, repository);
        manualScans = new ManualScanService(this, configRef::get, collector, pipeline);
        deepScans = new DeepScanService(this, configRef::get, collector, pipeline);
        reports = new ReportService(this, configRef::get);

        ChunkDoctorCommand command = new ChunkDoctorCommand(configRef::get, messages, pipeline, manualScans,
                deepScans, monitoring, repository, notifications, reports, this::reloadServices);
        PluginCommand pluginCommand = Objects.requireNonNull(getCommand("chunkdoctor"),
                "chunkdoctor command is missing from plugin.yml");
        pluginCommand.setExecutor(command);
        pluginCommand.setTabCompleter(command);
        getServer().getPluginManager().registerEvents(
                new WorldLifecycleListener(deepScans, manualScans, repository), this);

        MetricsBootstrap.start(this);
        monitoring.restartIfEnabled();
        getLogger().info("ChunkDoctor " + getPluginMeta().getVersion()
                + " enabled. Scores estimate risk; they do not measure per-chunk TPS.");
    }

    @Override
    public void onDisable() {
        if (monitoring != null) {
            monitoring.stop();
        }
        if (deepScans != null) {
            deepScans.cancelAll();
        }
        if (manualScans != null) {
            manualScans.cancelAll();
        }
        if (reports != null) {
            reports.close();
        }
        if (pipeline != null) {
            pipeline.close();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    private void reloadServices() {
        reloadConfig();
        loadValidatedConfig();
        monitoring.restartIfEnabled();
    }

    private void loadValidatedConfig() {
        ConfigLoader loader = new ConfigLoader(getLogger()::warning);
        configRef.set(loader.load(getConfig()));
    }

    private static void storeResult(ResultRepository repository, NotificationService notifications,
                                    ChunkAnalysisResult result) {
        ChunkAnalysisResult previous = repository.put(result);
        notifications.onResult(previous, result);
    }
}
