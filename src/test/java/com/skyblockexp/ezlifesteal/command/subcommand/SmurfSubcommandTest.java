package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmurfSubcommandTest {

    @Test
    void nonPlayerSender_getsPlayerOnlyMessage() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new SmurfSubcommand().execute(sender, null, "lifesteal", new String[]{"smurf"}, context);

        verify(sender).sendMessage("This command can only be used by players.");
    }

    @Test
    void permissionDenied_returnsEarly() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        Player sender = mock(Player.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new SmurfSubcommand().execute(sender, null, "lifesteal", new String[]{"smurf"}, context);

        verify(context).requirePermissionPublic(any(), eq("lifesteal.smurf.manage"), any());
        verify(context, never()).getPluginAccessorPublic();
    }
}
