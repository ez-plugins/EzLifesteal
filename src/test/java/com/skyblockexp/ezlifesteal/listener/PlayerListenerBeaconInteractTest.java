package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BanEnforcementService;
import com.skyblockexp.ezlifesteal.service.BeaconReviveService;
import java.util.logging.Logger;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlayerListenerBeaconInteractTest {

    @Test
    void onPlayerInteractCancelsEventWhenBeaconReviveHandlesInteraction() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService beaconReviveService = mock(BeaconReviveService.class);
        PlayerListener listener = new PlayerListener(plugin,
                mock(BanEnforcementService.class),
                null,
                null,
                null,
                beaconReviveService,
                "",
                "",
                false,
                0L);

        Player player = mock(Player.class);
        Block beacon = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(event.getClickedBlock()).thenReturn(beacon);
        when(beacon.getType()).thenReturn(Material.BEACON);
        when(beaconReviveService.tryHandleBeaconInteract(player, item, beacon)).thenReturn(true);

        listener.onPlayerInteract(event);

        verify(event).setCancelled(true);
        verify(plugin, never()).getHeartRegistry();
    }

    @Test
    void onPlayerInteractDoesNotCancelEventWhenBeaconReviveDoesNotHandle() {
        PluginAccessor plugin = basePlugin();
        BeaconReviveService beaconReviveService = mock(BeaconReviveService.class);
        PlayerListener listener = new PlayerListener(plugin,
                mock(BanEnforcementService.class),
                null,
                null,
                null,
                beaconReviveService,
                "",
                "",
                false,
                0L);

        Player player = mock(Player.class);
        Block beacon = mock(Block.class);
        ItemStack item = mock(ItemStack.class);
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getPlayer()).thenReturn(player);
        when(event.getItem()).thenReturn(item);
        when(event.getClickedBlock()).thenReturn(beacon);
        when(beacon.getType()).thenReturn(Material.BEACON);
        when(beaconReviveService.tryHandleBeaconInteract(player, item, beacon)).thenReturn(false);

        listener.onPlayerInteract(event);

        verify(event, never()).setCancelled(true);
    }

    private static PluginAccessor basePlugin() {
        PluginAccessor plugin = mock(PluginAccessor.class);
        when(plugin.getPlugin()).thenReturn(mock(org.bukkit.plugin.java.JavaPlugin.class));
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        return plugin;
    }
}
