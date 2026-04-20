package com.skyblockexp.ezlifesteal.config;

import java.util.HashMap;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageService {
    private final String prefix;

    private final Map<String, String> messages = new HashMap<>();


    public MessageService(String prefix) {
        this.prefix = translate(prefix == null ? "" : prefix);
    }

    public void register(String key, String message) {
        messages.put(key, translate(message));
    }

    public String getMessage(String key) {
        return messages.getOrDefault(key, "");
    }

    public void sendMessage(CommandSender sender, String key, Map<String, String> placeholders) {
        final String message = render(key, placeholders);
        if (message.isEmpty()) {
            return;
        }
        sender.sendMessage(prefix + message);
    }

    public void sendMessage(CommandSender sender, String key) {
        sendMessage(sender, key, null);
    }

    public String format(String key, Map<String, String> placeholders) {
        final String message = render(key, placeholders);
        return prefix + message;
    }

    public String getPrefix() {
        return prefix;
    }

    public String render(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        if (message.isEmpty()) {
            return "";
        }
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                message = message.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return message;
    }

    private String translate(String input) {
        return ChatColor.translateAlternateColorCodes('&', input == null ? "" : input);
    }
}
