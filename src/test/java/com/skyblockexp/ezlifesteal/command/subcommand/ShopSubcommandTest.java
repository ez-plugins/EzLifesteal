package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.gui.ShopGuiManager;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShopSubcommandTest {

    @Test
    void nonPlayerSender_getsPlayerOnlyMessage() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        CommandSender sender = mock(CommandSender.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ShopSubcommand().execute(sender, null, "lifesteal", new String[]{"shop"}, context);

        verify(sender).sendMessage("This command can only be used by players.");
        verify(context, never()).requirePermissionPublic(any(), anyString(), any());
    }

    @Test
    void playerWithNonEzPlugin_getsFailureMessage() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        Player sender = mock(Player.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        new ShopSubcommand().execute(sender, null, "lifesteal", new String[]{"shop"}, context);

        verify(context).getPluginAccessorPublic();
        verify(sender).sendMessage("Unable to open the shop right now.");
    }

    @Test
    void playerWithEzPlugin_opensShop() {
        LifestealCommand context = mock(LifestealCommand.class);
        PluginAccessor plugin = mock(PluginAccessor.class);
        EzLifestealPlugin ezLifestealPlugin = mock(EzLifestealPlugin.class);
        Player sender = mock(Player.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(plugin.getPlugin()).thenReturn(ezLifestealPlugin);

        try (MockedStatic<ShopGuiManager> shopGuiManager = mockStatic(ShopGuiManager.class)) {
            new ShopSubcommand().execute(sender, null, "lifesteal", new String[]{"shop"}, context);

            shopGuiManager.verify(() -> ShopGuiManager.openShop(ezLifestealPlugin, sender));
            verify(sender, never()).sendMessage("Unable to open the shop right now.");
        }
    }
}
