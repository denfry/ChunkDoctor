package dev.chunkdoctor.metrics;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class MetricsBootstrap {
    private static final String RESOURCE_NAME = "bstats.properties";

    private MetricsBootstrap() {
    }

    public static void start(JavaPlugin plugin) {
        int pluginId = readPluginId(plugin);
        if (pluginId <= 0) {
            plugin.getLogger().warning(
                    "bStats metrics are disabled because bstats_plugin_id is not configured for this build.");
            return;
        }

        new Metrics(plugin, pluginId);
    }

    static int readPluginId(JavaPlugin plugin) {
        try (InputStream input = plugin.getResource(RESOURCE_NAME)) {
            if (input == null) {
                plugin.getLogger().warning("Missing " + RESOURCE_NAME + "; bStats metrics will not start.");
                return 0;
            }

            Properties properties = new Properties();
            properties.load(input);
            String value = properties.getProperty("plugin-id", "").trim();
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            plugin.getLogger().warning("Invalid bStats plugin ID; metrics will not start.");
            return 0;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not read the bStats configuration: " + exception.getMessage());
            return 0;
        }
    }
}
