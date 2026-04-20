package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReloadSubcommandTest {

    @Test
    void permissionDenied_returnsWithoutReload() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(false);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ReloadSubcommand().execute(sender, null, "lifesteal", new String[]{"reload"}, context);

        verify(plugin, never()).reloadPlugin(any());
    }

    @Test
    void success_invokesReloadExactlyOnce() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.requirePermissionPublic(any(), anyString(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ReloadSubcommand().execute(sender, null, "lifesteal", new String[]{"reload"}, context);

        verify(plugin).reloadPlugin(sender);
    }
}
