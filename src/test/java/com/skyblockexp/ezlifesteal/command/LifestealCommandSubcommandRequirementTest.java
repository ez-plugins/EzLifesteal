package com.skyblockexp.ezlifesteal.command;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifestealCommandSubcommandRequirementTest {

    @Test
    void nonPlayerWithoutPermissions_noAllowedSubcommands() {
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.hasPermission(Mockito.anyString())).thenReturn(false);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(sender);

        assertNotNull(allowed);
        assertTrue(allowed.isEmpty(), "Expected no allowed subcommands for non-player without permissions");
    }

    @Test
    void playerWithoutPermissions_includesShop() {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission(Mockito.anyString())).thenReturn(false);

        List<String> allowed = LifestealCommand.listAllowedSubcommands(player);

        assertNotNull(allowed);
        assertTrue(allowed.contains("shop"), "Player should be allowed to use 'shop' subcommand");
    }

    @Test
    void playerWithAdminPermission_includesAdminCommands() {
        Player player = Mockito.mock(Player.class);
        Mockito.when(player.hasPermission(Mockito.eq("lifesteal.admin"))).thenReturn(true);
        Mockito.when(player.hasPermission(Mockito.anyString())).thenAnswer(invocation -> {
            String perm = invocation.getArgument(0);
            return "lifesteal.admin".equals(perm);
        });

        List<String> allowed = LifestealCommand.listAllowedSubcommands(player);

        assertNotNull(allowed);
        assertTrue(allowed.contains("resetall"), "Admin should see 'resetall'");
        assertTrue(allowed.contains("hologram"), "Admin should see 'hologram'");
        assertTrue(allowed.contains("test"), "Admin should see 'test'");
    }
}
