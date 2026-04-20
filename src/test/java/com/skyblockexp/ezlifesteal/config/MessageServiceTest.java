package com.skyblockexp.ezlifesteal.config;

import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MessageServiceTest {

    @Test
    void constructorTranslatesPrefixColorCodes() {
        MessageService service = new MessageService("&7[&aLife&cSteal&7] ");

        assertEquals("§7[§aLife§cSteal§7] ", service.getPrefix());
    }

    @Test
    void registerAndGetMessageReturnTranslatedMessageAndEmptyForUnknownKey() {
        MessageService service = new MessageService("&8[&bEZ&8] ");
        service.register("greet", "&aHello &bPlayer");

        assertEquals("§aHello §bPlayer", service.getMessage("greet"));
        assertEquals("", service.getMessage("missing"));
    }

    @Test
    void renderReplacesKnownPlaceholdersAndKeepsUnmatchedTokens() {
        MessageService service = new MessageService("&8[&bEZ&8] ");
        service.register("welcome", "Hello %player%, rank: %rank%, world: %world%.");

        String rendered = service.render("welcome", Map.of("player", "Alex", "world", "spawn"));

        assertEquals("Hello Alex, rank: %rank%, world: spawn.", rendered);
    }

    @Test
    void formatPrependsPrefixConsistently() {
        MessageService service = new MessageService("&7[&aLS&7] ");
        service.register("status", "&fLives: &c%lives%");

        String formatted = service.format("status", Map.of("lives", "3"));

        assertEquals("§7[§aLS§7] §fLives: §c3", formatted);
    }

    @Test
    void sendMessageSendsExactlyOnePrefixedMessageWhenKeyExists() {
        CommandSender sender = mock(CommandSender.class);
        MessageService service = new MessageService("&8[&bEZ&8] ");
        service.register("notify", "&aHi %player%");

        service.sendMessage(sender, "notify", Map.of("player", "Sam"));

        verify(sender, times(1)).sendMessage("§8[§bEZ§8] §aHi Sam");
    }

    @Test
    void sendMessageDoesNothingWhenKeyResolvesToEmptyMessage() {
        CommandSender sender = mock(CommandSender.class);
        MessageService service = new MessageService(ChatColor.GRAY + "[LS] ");

        service.sendMessage(sender, "missing", Map.of("player", "Sam"));

        verify(sender, never()).sendMessage(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void nullPlaceholdersAreHandledWithoutExceptions() {
        CommandSender sender = mock(CommandSender.class);
        MessageService service = new MessageService("&7[&aLS&7] ");
        service.register("plain", "&fNo placeholders");

        assertDoesNotThrow(() -> service.render("plain", null));
        assertDoesNotThrow(() -> service.format("plain", null));
        assertDoesNotThrow(() -> service.sendMessage(sender, "plain", null));
        verify(sender, times(1)).sendMessage("§7[§aLS§7] §fNo placeholders");
    }
}
