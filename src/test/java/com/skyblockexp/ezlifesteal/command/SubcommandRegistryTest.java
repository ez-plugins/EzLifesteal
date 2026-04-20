package com.skyblockexp.ezlifesteal.command;

import com.skyblockexp.ezlifesteal.command.subcommand.Subcommand;
import java.util.Collections;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubcommandRegistryTest {

    @Test
    void resolvesPrimaryAndAliasToSameSubcommand() {
        Subcommand subcommand = mock(Subcommand.class);
        SubcommandRegistry registry = new SubcommandRegistry()
                .register(
                        "smurf",
                        subcommand,
                        new LifestealCommand.SubcommandRequirement(true, List.of("lifesteal.smurf.manage"),
                                Collections.emptyMap()),
                        "sm"
                );

        assertSame(subcommand, registry.resolve("smurf").orElseThrow());
        assertSame(subcommand, registry.resolve("SM").orElseThrow());
    }

    @Test
    void unknownSubcommandSendsMessageAndReturnsHandled() {
        SubcommandRegistry registry = new SubcommandRegistry();
        LifestealCommand lifestealCommand = new LifestealCommand(null, registry);
        CommandSender sender = mock(CommandSender.class);

        boolean handled = lifestealCommand.onCommand(sender, mock(Command.class), "lifesteal", new String[]{"missing"});

        assertTrue(handled);
        verify(sender).sendMessage("Unknown subcommand: missing");
    }

    @Test
    void listAllowedSubcommandsAppliesPermissionAndPlayerGates() {
        SubcommandRegistry registry = new SubcommandRegistry();
        registry.register(
                "shop",
                mock(Subcommand.class),
                new LifestealCommand.SubcommandRequirement(true, Collections.emptyList(), Collections.emptyMap())
        );
        registry.register(
                "reload",
                mock(Subcommand.class),
                new LifestealCommand.SubcommandRequirement(false, List.of("lifesteal.manage.reload"),
                        Collections.emptyMap())
        );

        CommandSender console = mock(CommandSender.class);
        when(console.hasPermission(anyString())).thenReturn(false);
        Player player = mock(Player.class);
        when(player.hasPermission("lifesteal.manage.reload")).thenReturn(true);

        assertEquals(Collections.emptyList(), registry.listAllowedSubcommands(console));
        assertEquals(List.of("shop", "reload"), registry.listAllowedSubcommands(player));
    }
}
