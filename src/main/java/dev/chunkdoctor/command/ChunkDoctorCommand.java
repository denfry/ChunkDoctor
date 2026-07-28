package dev.chunkdoctor.command;

import dev.chunkdoctor.analysis.AnalysisPipeline;
import dev.chunkdoctor.analysis.DeepScanService;
import dev.chunkdoctor.analysis.ManualScanService;
import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.message.MessageService;
import dev.chunkdoctor.model.AnalysisReason;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.monitoring.MonitoringService;
import dev.chunkdoctor.monitoring.ResultRepository;
import dev.chunkdoctor.notification.NotificationService;
import dev.chunkdoctor.report.ReportService;
import dev.chunkdoctor.util.Pagination;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ChunkDoctorCommand implements CommandExecutor, TabCompleter {
    private static final int PAGE_SIZE = 8;
    private final Supplier<PluginConfig> config;
    private final MessageService messages;
    private final AnalysisPipeline pipeline;
    private final ManualScanService manualScans;
    private final DeepScanService deepScan;
    private final MonitoringService monitoring;
    private final ResultRepository repository;
    private final NotificationService notifications;
    private final ReportService reports;
    private final Runnable reloadAction;
    private final Map<String, Long> clearConfirmations = new HashMap<>();

    public ChunkDoctorCommand(Supplier<PluginConfig> config, MessageService messages,
                              AnalysisPipeline pipeline, ManualScanService manualScans,
                              DeepScanService deepScan, MonitoringService monitoring,
                              ResultRepository repository, NotificationService notifications,
                              ReportService reports, Runnable reloadAction) {
        this.config = config;
        this.messages = messages;
        this.pipeline = pipeline;
        this.manualScans = manualScans;
        this.deepScan = deepScan;
        this.monitoring = monitoring;
        this.repository = repository;
        this.notifications = notifications;
        this.reports = reports;
        this.reloadAction = reloadAction;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("chunkdoctor.use")) {
            messages.send(sender, "no-permission");
            return true;
        }
        String subcommand = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        return switch (subcommand) {
            case "help" -> help(sender);
            case "status" -> status(sender);
            case "scan" -> scan(sender, args);
            case "deep" -> deep(sender);
            case "top" -> top(sender, args);
            case "info" -> info(sender, args);
            case "teleport" -> teleport(sender, args);
            case "export" -> export(sender, args);
            case "reload" -> reload(sender);
            case "start" -> start(sender);
            case "stop" -> stop(sender);
            case "clear" -> clear(sender, args);
            case "notify" -> notifyToggle(sender);
            default -> {
                messages.send(sender, "unknown-command");
                yield true;
            }
        };
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(messages.parse("""
                <gold><bold>ChunkDoctor</bold></gold> <gray>— оценка потенциальной нагрузки</gray>
                <yellow>/cd status</yellow> <gray>— состояние мониторинга</gray>
                <yellow>/cd scan [radius]</yellow> <gray>— быстрый скан загруженных чанков</gray>
                <yellow>/cd deep</yellow> <gray>— глубокий скан текущего чанка</gray>
                <yellow>/cd top [page]</yellow> <gray>— рейтинг риска</gray>
                <yellow>/cd info <world> <x> <z></yellow>
                <yellow>/cd teleport <world> <x> <z></yellow>
                <yellow>/cd export [world x z]</yellow>
                <yellow>/cd notify</yellow> <gray>— переключить уведомления</gray>
                <yellow>/cd reload|start|stop|clear</yellow>
                """));
        return true;
    }

    private boolean status(CommandSender sender) {
        PluginConfig.Monitoring budget = config.get().monitoring();
        sender.sendMessage(messages.parse("""
                <gold><bold>ChunkDoctor status</bold></gold>
                <gray>Мониторинг:</gray> <state>
                <gray>TPS-пауза:</gray> <pause>
                <gray>Результаты:</gray> <white><results></white>
                <gray>Очередь:</gray> <white><queue>/<maximum></white>
                <gray>Глубокие сканы:</gray> <white><deep></white>
                <gray>Быстрые сканы:</gray> <white><manual></white>
                <gray>Бюджет:</gray> <white><chunks> чанков, <millis> мс / цикл</white>
                """,
                MessageService.text("state", monitoring.isRunning() ? "активен" : "остановлен"),
                MessageService.text("pause", monitoring.isTpsPaused() ? "да" : "нет"),
                MessageService.text("results", Integer.toString(repository.size())),
                MessageService.text("queue", Integer.toString(pipeline.pendingCount())),
                MessageService.text("maximum", Integer.toString(budget.maximumPendingAnalyses())),
                MessageService.text("deep", Integer.toString(deepScan.activeCount())),
                MessageService.text("manual", Integer.toString(manualScans.activeCount())),
                MessageService.text("chunks", Integer.toString(budget.chunksPerCycle())),
                MessageService.text("millis", Double.toString(budget.maxMillisecondsPerTick()))));
        return true;
    }

    private boolean scan(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.scan") || !(sender instanceof Player player)) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
            }
            return true;
        }
        int radius = args.length >= 2 ? parseInt(args[1], -1) : 0;
        if (radius < 0 || radius > config.get().manualScan().maximumRadius()) {
            messages.send(sender, "invalid-number");
            return true;
        }
        ManualScanService.StartResult started = manualScans.start(player, radius,
                result -> sendCompactResult(sender, result),
                () -> sender.sendMessage(Component.text("Быстрое сканирование завершено.", NamedTextColor.GREEN)));
        if (!started.accepted()) {
            sender.sendMessage(Component.text("У вас уже выполняется быстрый скан.", NamedTextColor.RED));
        } else if (started.loadedChunks() == 0) {
            messages.send(sender, "no-loaded-chunks");
        } else {
            messages.send(sender, "scan-started");
            sender.sendMessage(Component.text("Принято загруженных чанков: " + started.loadedChunks(),
                    NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean deep(CommandSender sender) {
        if (!require(sender, "chunkdoctor.deep") || !(sender instanceof Player player)) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
            }
            return true;
        }
        if (!deepScan.start(player, result -> {
            player.sendMessage(Component.text("Глубокий анализ завершён.", NamedTextColor.GREEN));
            sendDetailedResult(player, result);
        })) {
            sender.sendMessage(Component.text("Глубокий скан отключён, уже выполняется или достигнут лимит.", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Глубокий скан запущен с тик-бюджетом.", NamedTextColor.GREEN));
        }
        return true;
    }

    private boolean top(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.top")) {
            return true;
        }
        int pageNumber = args.length >= 2 ? parseInt(args[1], -1) : 1;
        if (pageNumber < 1) {
            messages.send(sender, "invalid-number");
            return true;
        }
        Pagination.Page<ChunkAnalysisResult> page = Pagination.page(repository.ranked(), pageNumber, PAGE_SIZE);
        sender.sendMessage(Component.text("ChunkDoctor top — " + page.number() + "/" + page.totalPages(),
                NamedTextColor.GOLD));
        if (page.items().isEmpty()) {
            sender.sendMessage(Component.text("Результатов пока нет.", NamedTextColor.GRAY));
        }
        page.items().forEach(result -> sendCompactResult(sender, result));
        Component navigation = Component.empty();
        if (page.number() > 1) {
            navigation = navigation.append(Component.text("[←]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/chunkdoctor top " + (page.number() - 1))));
        }
        if (page.number() < page.totalPages()) {
            navigation = navigation.append(Component.space()).append(Component.text("[→]", NamedTextColor.YELLOW)
                    .clickEvent(ClickEvent.runCommand("/chunkdoctor top " + (page.number() + 1))));
        }
        sender.sendMessage(navigation);
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.info")) {
            return true;
        }
        Optional<ChunkAnalysisResult> result = findResult(args, 1);
        if (result.isEmpty()) {
            sender.sendMessage(Component.text("Результат не найден или аргументы некорректны.", NamedTextColor.RED));
        } else {
            sendDetailedResult(sender, result.get());
        }
        return true;
    }

    private boolean teleport(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.teleport") || !(sender instanceof Player player)) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
            }
            return true;
        }
        ParsedChunk parsed = parseChunk(args, 1);
        if (parsed == null || !parsed.world().isChunkLoaded(parsed.x(), parsed.z())) {
            sender.sendMessage(Component.text("Мир или загруженный чанк не найден.", NamedTextColor.RED));
            return true;
        }
        Location safe = safeLocation(parsed.world(), parsed.x(), parsed.z());
        if (safe == null) {
            sender.sendMessage(Component.text("Безопасная точка телепортации не найдена.", NamedTextColor.RED));
            return true;
        }
        player.teleport(safe);
        sender.sendMessage(Component.text("Телепортация выполнена.", NamedTextColor.GREEN));
        return true;
    }

    private boolean export(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.export")) {
            return true;
        }
        List<ChunkAnalysisResult> selected;
        String suffix;
        if (args.length == 1) {
            selected = repository.ranked();
            suffix = "all";
        } else {
            Optional<ChunkAnalysisResult> result = findResult(args, 1);
            if (result.isEmpty()) {
                sender.sendMessage(Component.text("Результат не найден или аргументы некорректны.", NamedTextColor.RED));
                return true;
            }
            selected = List.of(result.get());
            suffix = result.get().key().worldName() + "-" + result.get().key().chunkX()
                    + "-" + result.get().key().chunkZ();
        }
        boolean accepted = reports.export(selected, suffix,
                path -> exportSuccess(sender, path), () -> messages.send(sender, "export-failed"));
        if (!accepted) {
            sender.sendMessage(Component.text("Очередь экспорта заполнена.", NamedTextColor.RED));
        } else {
            sender.sendMessage(Component.text("Экспорт выполняется асинхронно.", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!require(sender, "chunkdoctor.reload")) {
            return true;
        }
        reloadAction.run();
        messages.send(sender, "config-reloaded");
        return true;
    }

    private boolean start(CommandSender sender) {
        if (!require(sender, "chunkdoctor.control")) {
            return true;
        }
        monitoring.start();
        messages.send(sender, "monitoring-started");
        return true;
    }

    private boolean stop(CommandSender sender) {
        if (!require(sender, "chunkdoctor.control")) {
            return true;
        }
        monitoring.stop();
        messages.send(sender, "monitoring-stopped");
        return true;
    }

    private boolean clear(CommandSender sender, String[] args) {
        if (!require(sender, "chunkdoctor.clear")) {
            return true;
        }
        String identity = sender instanceof Player player ? player.getUniqueId().toString() : "console";
        long now = System.currentTimeMillis();
        if (args.length < 2 || !"confirm".equalsIgnoreCase(args[1])
                || now - clearConfirmations.getOrDefault(identity, 0L) > 30_000) {
            clearConfirmations.put(identity, now);
            messages.send(sender, "clear-confirm");
            return true;
        }
        repository.clear();
        clearConfirmations.remove(identity);
        messages.send(sender, "cache-cleared");
        return true;
    }

    private boolean notifyToggle(CommandSender sender) {
        if (!require(sender, "chunkdoctor.notify") || !(sender instanceof Player player)) {
            if (!(sender instanceof Player)) {
                messages.send(sender, "player-only");
            }
            return true;
        }
        boolean enabled = notifications.toggle(player);
        sender.sendMessage(Component.text("Уведомления " + (enabled ? "включены." : "отключены."),
                enabled ? NamedTextColor.GREEN : NamedTextColor.YELLOW));
        return true;
    }

    private void sendCompactResult(CommandSender sender, ChunkAnalysisResult result) {
        Component location = Component.text(result.key().display(), NamedTextColor.YELLOW)
                .hoverEvent(HoverEvent.showText(Component.text("Открыть подробный отчёт")))
                .clickEvent(ClickEvent.runCommand("/chunkdoctor info " + result.key().worldName()
                        + " " + result.key().chunkX() + " " + result.key().chunkZ()));
        Component teleport = Component.text(" [TP]", NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(Component.text("Безопасная телепортация")))
                .clickEvent(ClickEvent.runCommand("/chunkdoctor teleport " + result.key().worldName()
                        + " " + result.key().chunkX() + " " + result.key().chunkZ()));
        sender.sendMessage(Component.text(result.riskScore() + "/100 " + result.riskLevel() + " ",
                        color(result.riskScore()))
                .append(location).append(teleport));
    }

    private void sendDetailedResult(CommandSender sender, ChunkAnalysisResult result) {
        sender.sendMessage(Component.text("ChunkDoctor — " + result.key().display(), NamedTextColor.GOLD));
        sender.sendMessage(Component.text("Risk score: " + result.riskScore() + "/100 | "
                + result.riskLevel() + " | confidence " + result.confidence(), color(result.riskScore())));
        sender.sendMessage(Component.text("Главные причины:", NamedTextColor.YELLOW));
        if (result.reasons().isEmpty()) {
            sender.sendMessage(Component.text("- значимых факторов не найдено", NamedTextColor.GRAY));
        }
        for (AnalysisReason reason : result.reasons()) {
            sender.sendMessage(Component.text("- " + reason.message(), NamedTextColor.GRAY));
        }
        if (!result.recommendations().isEmpty()) {
            sender.sendMessage(Component.text("Рекомендации:", NamedTextColor.YELLOW));
            result.recommendations().forEach(value ->
                    sender.sendMessage(Component.text("- " + value, NamedTextColor.GRAY)));
        }
    }

    private Optional<ChunkAnalysisResult> findResult(String[] args, int offset) {
        ParsedChunk parsed = parseChunk(args, offset);
        if (parsed == null) {
            return Optional.empty();
        }
        return repository.get(new ChunkKey(parsed.world().getUID(), parsed.world().getName(), parsed.x(), parsed.z()));
    }

    private ParsedChunk parseChunk(String[] args, int offset) {
        if (args.length != offset + 3) {
            return null;
        }
        World world = org.bukkit.Bukkit.getWorld(args[offset]);
        int x = parseInt(args[offset + 1], Integer.MIN_VALUE);
        int z = parseInt(args[offset + 2], Integer.MIN_VALUE);
        if (world == null || x == Integer.MIN_VALUE || z == Integer.MIN_VALUE) {
            return null;
        }
        return new ParsedChunk(world, x, z);
    }

    private Location safeLocation(World world, int chunkX, int chunkZ) {
        int x = chunkX * 16 + 8;
        int z = chunkZ * 16 + 8;
        int top = world.getHighestBlockYAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        for (int y = Math.min(world.getMaxHeight() - 3, top + 1);
             y >= Math.max(world.getMinHeight() + 1, top - 8); y--) {
            Material floor = world.getBlockAt(x, y - 1, z).getType();
            if (floor.isSolid() && floor != Material.MAGMA_BLOCK
                    && world.getBlockAt(x, y, z).isPassable()
                    && world.getBlockAt(x, y + 1, z).isPassable()
                    && !world.getBlockAt(x, y, z).isLiquid()
                    && !world.getBlockAt(x, y + 1, z).isLiquid()) {
                return new Location(world, x + 0.5, y, z + 0.5, 0.0f, 0.0f);
            }
        }
        return null;
    }

    private boolean require(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            messages.send(sender, "no-permission");
            return false;
        }
        return true;
    }

    private void exportSuccess(CommandSender sender, Path path) {
        messages.send(sender, "export-complete", MessageService.text("file", path.toString()));
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static NamedTextColor color(int score) {
        if (score >= 80) {
            return NamedTextColor.RED;
        }
        if (score >= 60) {
            return NamedTextColor.GOLD;
        }
        if (score >= 30) {
            return NamedTextColor.YELLOW;
        }
        return NamedTextColor.GREEN;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission("chunkdoctor.use")) {
            return List.of();
        }
        if (args.length == 1) {
            List<String> commands = new ArrayList<>();
            addIf(sender, commands, "help", "chunkdoctor.use");
            addIf(sender, commands, "status", "chunkdoctor.use");
            addIf(sender, commands, "scan", "chunkdoctor.scan");
            addIf(sender, commands, "deep", "chunkdoctor.deep");
            addIf(sender, commands, "top", "chunkdoctor.top");
            addIf(sender, commands, "info", "chunkdoctor.info");
            addIf(sender, commands, "teleport", "chunkdoctor.teleport");
            addIf(sender, commands, "export", "chunkdoctor.export");
            addIf(sender, commands, "reload", "chunkdoctor.reload");
            addIf(sender, commands, "start", "chunkdoctor.control");
            addIf(sender, commands, "stop", "chunkdoctor.control");
            addIf(sender, commands, "clear", "chunkdoctor.clear");
            addIf(sender, commands, "notify", "chunkdoctor.notify");
            return filter(commands, args[0]);
        }
        if (args.length == 2 && List.of("info", "teleport", "export").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(org.bukkit.Bukkit.getWorlds().stream().map(World::getName).toList(), args[1]);
        }
        if (args.length == 2 && "clear".equalsIgnoreCase(args[0])) {
            return filter(List.of("confirm"), args[1]);
        }
        return List.of();
    }

    private static void addIf(CommandSender sender, List<String> target, String value, String permission) {
        if (sender.hasPermission(permission)) {
            target.add(value);
        }
    }

    private static List<String> filter(List<String> values, String prefix) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private record ParsedChunk(World world, int x, int z) {
    }
}
