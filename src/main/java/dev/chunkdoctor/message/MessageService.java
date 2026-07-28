package dev.chunkdoctor.message;

import dev.chunkdoctor.config.PluginConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

import java.util.function.Supplier;

public final class MessageService {
    private final Supplier<PluginConfig> config;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    public MessageService(Supplier<PluginConfig> config) {
        this.config = config;
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        sender.sendMessage(message(key, resolvers));
    }

    public Component message(String key, TagResolver... resolvers) {
        String prefix = config.get().messages().getOrDefault("prefix", "");
        String body = config.get().messages().getOrDefault(key, "<red>Missing message: " + key + "</red>");
        return miniMessage.deserialize(prefix + body, resolvers);
    }

    public Component parse(String value, TagResolver... resolvers) {
        return miniMessage.deserialize(value, resolvers);
    }

    public static TagResolver text(String key, String value) {
        return Placeholder.unparsed(key, value);
    }
}
