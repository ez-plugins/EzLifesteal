package com.skyblockexp.ezlifesteal.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

public final class ActionBarHelper {

    private ActionBarHelper() {
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isEmpty()) {
            return;
        }
        try {
            player.sendActionBar(message);
        }
        catch (NoSuchMethodError | NoClassDefFoundError ignored) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
        }
    }
}
