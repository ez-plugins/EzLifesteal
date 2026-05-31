package com.skyblockexp.ezlifesteal.command.subcommand;

import com.skyblockexp.ezlifesteal.command.LifestealCommand;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.hologram.TopHologramManager;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HologramSubcommandTest {

    @Test
    void placeAction_whenPlaced_sendsPlacedMessage() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getTopHologramManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);

        Player player = Mockito.mock(Player.class);
        World mockWorld = Mockito.mock(World.class);
        Location loc = new Location(mockWorld, 1.5, 64, 2.5);
        loc.setYaw(0f);
        when(player.getLocation()).thenReturn(loc);

        when(mockWorld.getName()).thenReturn("testworld");
        Location base = new Location(mockWorld, 10, 64, 10);
        when(manager.place(any(Location.class))).thenReturn(true);
        when(manager.getLocation()).thenReturn(base);

        LifestealCommand context = Mockito.spy(new LifestealCommand());
        Mockito.doReturn(true).when(context).requirePermissionPublic(any(), anyString(), any(), any());
        Mockito.doReturn(plugin).when(context).getPluginAccessorPublic();
        Mockito.doReturn("1").when(context).formatCoordinate(anyDouble());

        // sanity: context returns the plugin accessor
        org.junit.jupiter.api.Assertions.assertSame(plugin, context.getPluginAccessorPublic());

        HologramSubcommand sub = new HologramSubcommand();
        boolean result = sub.execute(player, Mockito.mock(Command.class), "lifesteal", new String[]{"hologram",
            "place"}, context);

        assert result;
        verify(manager).place(any(Location.class));
        verify(manager).getLocation();
        verify(plugin).getMessageService();
    }

    @Test
    void removeAction_whenRemoved_sendsRemovedMessage() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getTopHologramManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);

        CommandSender sender = Mockito.mock(CommandSender.class);
        when(manager.remove()).thenReturn(true);

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        HologramSubcommand sub = new HologramSubcommand();
        boolean result = sub.execute(sender, Mockito.mock(Command.class), "lifesteal", new String[]{"hologram",
            "remove"}, context);

        assert result;
        verify(manager).remove();
        verify(plugin).getMessageService();
    }

    @Test
    void cleanupAction_removesOrphansAndSendsCountMessage() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getTopHologramManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);
        when(manager.removeNearbyOrphans(any(Location.class), anyDouble())).thenReturn(3);

        Player player = Mockito.mock(Player.class);
        World mockWorld = Mockito.mock(World.class);
        Location loc = new Location(mockWorld, 0, 64, 0);
        when(player.getLocation()).thenReturn(loc);

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.formatCoordinate(anyDouble())).thenReturn("10");

        HologramSubcommand sub = new HologramSubcommand();
        boolean result = sub.execute(player, Mockito.mock(Command.class), "lifesteal",
                new String[]{"hologram", "cleanup"}, context);

        assert result;
        verify(manager).removeNearbyOrphans(any(Location.class), anyDouble());
        verify(msg).sendMessage(any(), Mockito.eq("hologram-cleanup-removed"), any());
    }

    @Test
    void cleanupAction_noOrphans_sendsNoneMessage() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getTopHologramManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);
        when(manager.removeNearbyOrphans(any(Location.class), anyDouble())).thenReturn(0);

        Player player = Mockito.mock(Player.class);
        World mockWorld = Mockito.mock(World.class);
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.formatCoordinate(anyDouble())).thenReturn("10");

        HologramSubcommand sub = new HologramSubcommand();
        boolean result = sub.execute(player, Mockito.mock(Command.class), "lifesteal",
                new String[]{"hologram", "cleanup"}, context);

        assert result;
        verify(msg).sendMessage(any(), Mockito.eq("hologram-cleanup-none"), any());
    }

    @Test
    void cleanupAction_requiresPlayerSender() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        when(plugin.getTopHologramManager()).thenReturn(manager);

        CommandSender consoleSender = Mockito.mock(CommandSender.class);

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);

        HologramSubcommand sub = new HologramSubcommand();
        boolean result = sub.execute(consoleSender, Mockito.mock(Command.class), "lifesteal",
                new String[]{"hologram", "cleanup"}, context);

        assert result;
        verify(consoleSender).sendMessage(Mockito.contains("players"));
        Mockito.verifyNoInteractions(manager);
    }

    @Test
    void cleanupAction_customRadius_isPassedThrough() {
        PluginAccessor plugin = Mockito.mock(PluginAccessor.class);
        TopHologramManager manager = Mockito.mock(TopHologramManager.class);
        MessageService msg = Mockito.mock(MessageService.class);

        when(plugin.getTopHologramManager()).thenReturn(manager);
        when(plugin.getMessageService()).thenReturn(msg);
        when(manager.removeNearbyOrphans(any(Location.class), anyDouble())).thenReturn(0);

        Player player = Mockito.mock(Player.class);
        World mockWorld = Mockito.mock(World.class);
        when(player.getLocation()).thenReturn(new Location(mockWorld, 0, 64, 0));

        LifestealCommand context = Mockito.mock(LifestealCommand.class);
        when(context.requirePermissionPublic(any(), anyString(), any(), any())).thenReturn(true);
        when(context.getPluginAccessorPublic()).thenReturn(plugin);
        when(context.formatCoordinate(anyDouble())).thenReturn("20");

        HologramSubcommand sub = new HologramSubcommand();
        sub.execute(player, Mockito.mock(Command.class), "lifesteal",
                new String[]{"hologram", "cleanup", "20"}, context);

        verify(manager).removeNearbyOrphans(any(Location.class), Mockito.eq(20.0));
    }
}
