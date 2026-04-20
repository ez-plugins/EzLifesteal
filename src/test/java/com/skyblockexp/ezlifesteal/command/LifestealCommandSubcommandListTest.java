package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LifestealCommandSubcommandListTest {

    @Test
    void listAllowedSubcommands_forPlayerWithNoPermissions_includesOnlyPlayerOnlyShop() {
        Player player = mock(Player.class);
        when(player.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(false);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(player);

        // 'shop' is player-only and requires no permissions; most other commands require permissions
        assertTrue(allowed.contains("shop"));
        // ensure at least one entry and that 'shop' is present
        assertFalse(allowed.isEmpty());
        // ensure that an admin-only command is not present
        assertFalse(allowed.contains("resetall"));
    }

    @Test
    void listAllowedSubcommands_forAdminPlayer_includesAdminCommands() {
        Player admin = mock(Player.class);
        // grant admin permission
        when(admin.hasPermission("lifesteal.admin")).thenReturn(true);
        when(admin.hasPermission(org.mockito.ArgumentMatchers.anyString())).thenReturn(true);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(admin);

        // admin should be able to execute a variety of commands
        assertTrue(allowed.contains("hearts"));
        assertTrue(allowed.contains("resetall"));
        assertTrue(allowed.contains("shop"));
        assertTrue(allowed.contains("hologram"));
    }
}
