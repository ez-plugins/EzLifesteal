package com.skyblockexp.ezlifesteal.util;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ActionBarHelperTest {

    @Test
    void sendActionBarIsNoOpForNullPlayer() {
        assertDoesNotThrow(() -> ActionBarHelper.sendActionBar(null, "hello"));
    }

    @Test
    void sendActionBarIsNoOpForEmptyOrNullMessage() {
        Player player = mock(Player.class);

        ActionBarHelper.sendActionBar(player, "");
        ActionBarHelper.sendActionBar(player, null);

        verify(player, never()).sendActionBar(any(String.class));
        verify(player, never()).spigot();
    }

    @Test
    void sendActionBarUsesPrimaryPlayerApiWhenAvailable() {
        Player player = mock(Player.class);
        String message = "Action bar text";

        ActionBarHelper.sendActionBar(player, message);

        verify(player).sendActionBar(message);
        verify(player, never()).spigot();
    }

    @Test
    void sendActionBarFallsBackToSpigotApiWhenPrimaryMethodIsUnavailable() {
        Player player = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        String message = "Fallback message";
        doThrow(new NoSuchMethodError("missing sendActionBar")).when(player).sendActionBar(message);

        org.mockito.Mockito.when(player.spigot()).thenReturn(spigot);

        ActionBarHelper.sendActionBar(player, message);

        verify(player).sendActionBar(message);
        verify(spigot).sendMessage(eq(ChatMessageType.ACTION_BAR), any(BaseComponent[].class));
    }

    @Test
    void sendActionBarPreservesFormattingInputWithoutMutatingText() {
        Player player = mock(Player.class);
        String formatted = "&aGreen §lBold";

        ActionBarHelper.sendActionBar(player, formatted);

        verify(player).sendActionBar(formatted);
    }

    @Test
    void fallbackPathConvertsLegacyFormattingForSpigotComponents() {
        Player player = mock(Player.class);
        Player.Spigot spigot = mock(Player.Spigot.class);
        String message = "§aGreen";
        doThrow(new NoClassDefFoundError("missing api class")).when(player).sendActionBar(message);
        org.mockito.Mockito.when(player.spigot()).thenReturn(spigot);

        ActionBarHelper.sendActionBar(player, message);

        org.mockito.ArgumentCaptor<BaseComponent[]> captor = org.mockito.ArgumentCaptor.forClass(BaseComponent[].class);
        verify(spigot).sendMessage(eq(ChatMessageType.ACTION_BAR), captor.capture());
        BaseComponent[] components = captor.getValue();
        assertEquals(1, components.length);
        assertEquals("Green", components[0].toPlainText());
    }
}
