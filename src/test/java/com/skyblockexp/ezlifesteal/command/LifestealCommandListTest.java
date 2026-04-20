package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifestealCommandListTest {

    @Test
    void listAllowedSubcommands_for_player_with_all_permissions() {
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(player);
        assertNotNull(allowed);
        // player with all permissions should be allowed to use player-only commands like shop and transfer
        assertTrue(allowed.contains("shop"));
        assertTrue(allowed.contains("transfer") || allowed.contains("top"));
    }

    @Test
    void listAllowedSubcommands_for_console_without_permissions_excludes_player_only() {
        CommandSender sender = mock(CommandSender.class);
        when(sender.hasPermission(anyString())).thenReturn(false);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(sender);
        assertNotNull(allowed);
        // console (not a Player) must not be allowed player-only 'shop'
        assertFalse(allowed.contains("shop"));
    }
}
