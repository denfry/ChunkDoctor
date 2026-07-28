package dev.chunkdoctor.notification;

import dev.chunkdoctor.config.PluginConfig;
import dev.chunkdoctor.message.MessageService;
import dev.chunkdoctor.model.ChunkAnalysisResult;
import dev.chunkdoctor.model.ChunkKey;
import dev.chunkdoctor.model.RiskLevel;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class NotificationService {
    private final JavaPlugin plugin;
    private final Supplier<PluginConfig> config;
    private final MessageService messages;
    private final NamespacedKey disabledKey;
    private final Map<ChunkKey, Long> cooldowns = new HashMap<>();

    public NotificationService(JavaPlugin plugin, Supplier<PluginConfig> config, MessageService messages) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.disabledKey = new NamespacedKey(plugin, "notifications_disabled");
    }

    public void onResult(ChunkAnalysisResult previous, ChunkAnalysisResult current) {
        PluginConfig.Notifications settings = config.get().notifications();
        if (!settings.enabled()) {
            return;
        }
        boolean newCritical = previous == null && current.riskLevel() == RiskLevel.CRITICAL;
        boolean increased = previous != null
                && current.riskScore() - previous.riskScore() >= settings.significantIncrease();
        boolean deepCompleted = current.deepScan();
        boolean eligibleLevel = !settings.criticalOnly() || current.riskLevel() == RiskLevel.CRITICAL;
        if (!(deepCompleted || newCritical || (increased && eligibleLevel))) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!deepCompleted && now - cooldowns.getOrDefault(current.key(), 0L) < settings.cooldownMillis()) {
            return;
        }
        cooldowns.put(current.key(), now);
        Component coordinates = Component.text(current.key().display(), NamedTextColor.GOLD)
                .hoverEvent(HoverEvent.showText(Component.text("Открыть подробности")))
                .clickEvent(ClickEvent.runCommand("/chunkdoctor info " + current.key().worldName()
                        + " " + current.key().chunkX() + " " + current.key().chunkZ()));
        Component notice = messages.parse("<red><bold>Риск <score>/100</bold></red> <gray>в</gray> ",
                        MessageService.text("score", Integer.toString(current.riskScore())))
                .append(coordinates);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.hasPermission("chunkdoctor.notify") && notificationsEnabled(player)) {
                player.sendMessage(notice);
            }
        }
    }

    public boolean toggle(Player player) {
        boolean enabled = notificationsEnabled(player);
        if (enabled) {
            player.getPersistentDataContainer().set(disabledKey, PersistentDataType.BYTE, (byte) 1);
        } else {
            player.getPersistentDataContainer().remove(disabledKey);
        }
        return !enabled;
    }

    public boolean notificationsEnabled(Player player) {
        return !player.getPersistentDataContainer().has(disabledKey, PersistentDataType.BYTE);
    }
}
