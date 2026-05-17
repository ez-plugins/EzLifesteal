package com.skyblockexp.ezlifesteal.listener;

import com.skyblockexp.ezcountdown.api.event.CountdownEndEvent;
import com.skyblockexp.ezcountdown.api.model.Countdown;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.service.BeaconSpawnService;
import com.skyblockexp.ezlifesteal.util.ban.PlatformBanAdapter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SpawnedBeaconListenerTest {

    private ServerMock server;
    private WorldMock world;
    private BeaconSpawnService beaconSpawnService;
    private PluginAccessor accessor;
    private MessageService messageService;
    private Logger logger;
    private SpawnedBeaconListener listener;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("world");
        beaconSpawnService = mock(BeaconSpawnService.class);
        accessor = mock(PluginAccessor.class);
        messageService = mock(MessageService.class);
        logger = mock(Logger.class);

        when(accessor.getMessageService()).thenReturn(messageService);
        when(accessor.getBanAdapter()).thenReturn(mock(PlatformBanAdapter.class));
        when(messageService.getMessage(anyString())).thenReturn("protected");

        listener = new SpawnedBeaconListener(beaconSpawnService, accessor, logger);
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    // -------------------------------------------------------------------------
    // onCountdownEnd
    // -------------------------------------------------------------------------

    @Test
    void onCountdownEnd_nonBeaconCountdown_noInteraction() {
        Countdown countdown = mock(Countdown.class);
        when(countdown.getName()).thenReturn("some-other-countdown");
        CountdownEndEvent event = new CountdownEndEvent(countdown);

        listener.onCountdownEnd(event);

        verify(beaconSpawnService, never()).markAvailable(any(), any());
    }

    @Test
    void onCountdownEnd_validBeaconCountdown_callsMarkAvailable() {
        UUID beaconId = UUID.randomUUID();
        String shortId = beaconId.toString().substring(0, 8);

        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.shortId()).thenReturn(shortId);
        when(beacon.getId()).thenReturn(beaconId);
        when(beaconSpawnService.getActiveBeacons()).thenReturn(List.of(beacon));

        Countdown countdown = mock(Countdown.class);
        when(countdown.getName()).thenReturn("ezls-beacon-" + shortId);
        CountdownEndEvent event = new CountdownEndEvent(countdown);

        listener.onCountdownEnd(event);

        verify(beaconSpawnService).markAvailable(beaconId, accessor);
    }

    @Test
    void onCountdownEnd_beaconPrefixButNoMatchingBeacon_noMarkAvailable() {
        when(beaconSpawnService.getActiveBeacons()).thenReturn(List.of());

        Countdown countdown = mock(Countdown.class);
        when(countdown.getName()).thenReturn("ezls-beacon-abc12345");
        CountdownEndEvent event = new CountdownEndEvent(countdown);

        listener.onCountdownEnd(event);

        verify(beaconSpawnService, never()).markAvailable(any(), any());
    }

    // -------------------------------------------------------------------------
    // onBeaconRightClick
    // -------------------------------------------------------------------------

    @Test
    void onBeaconRightClick_nonRightClickAction_ignores() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.LEFT_CLICK_BLOCK);

        listener.onBeaconRightClick(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onBeaconRightClick_nullBlock_ignores() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(null);

        listener.onBeaconRightClick(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onBeaconRightClick_nonBeaconBlock_ignores() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Block block = mock(Block.class);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.STONE);

        listener.onBeaconRightClick(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onBeaconRightClick_beaconNotInRepo_ignores() {
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Block block = mock(Block.class);
        Location loc = new Location(world, 10, 64, 10);
        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(block.getType()).thenReturn(Material.BEACON);
        when(block.getLocation()).thenReturn(loc);
        when(beaconSpawnService.findByLocation(loc)).thenReturn(Optional.empty());

        listener.onBeaconRightClick(event);

        verify(event, never()).setCancelled(true);
    }

    @Test
    void onBeaconRightClick_beaconInRepo_cancelsEvent() {
        PlayerMock player = server.addPlayer();
        PlayerInteractEvent event = mock(PlayerInteractEvent.class);
        Block block = mock(Block.class);
        Location loc = new Location(world, 10, 64, 10);

        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(beacon.getLocation()).thenReturn(loc);
        when(beacon.getStatus()).thenReturn(SpawnedBeaconStatus.AVAILABLE);
        when(beacon.getId()).thenReturn(UUID.randomUUID());
        when(beacon.shortId()).thenReturn("abc12345");
        when(beacon.getSpawnedAtMillis()).thenReturn(System.currentTimeMillis());
        when(beacon.getExpiresAtMillis()).thenReturn(0L);

        when(event.getAction()).thenReturn(Action.RIGHT_CLICK_BLOCK);
        when(event.getClickedBlock()).thenReturn(block);
        when(event.getPlayer()).thenReturn(player);
        when(block.getType()).thenReturn(Material.BEACON);
        when(block.getLocation()).thenReturn(loc);
        when(beaconSpawnService.findByLocation(loc)).thenReturn(Optional.of(beacon));

        listener.onBeaconRightClick(event);

        verify(event).setCancelled(true);
    }

    // -------------------------------------------------------------------------
    // onBlockBreak
    // -------------------------------------------------------------------------

    @Test
    void onBlockBreak_nonBeacon_doesNotCancel() {
        PlayerMock player = server.addPlayer();
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void onBlockBreak_beaconNotInRepo_doesNotCancel() {
        PlayerMock player = server.addPlayer();
        Block block = mock(Block.class);
        Location loc = new Location(world, 5, 64, 5);
        when(block.getType()).thenReturn(Material.BEACON);
        when(block.getLocation()).thenReturn(loc);
        when(beaconSpawnService.findByLocation(loc)).thenReturn(Optional.empty());
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBlockBreak(event);

        assertFalse(event.isCancelled());
    }

    @Test
    void onBlockBreak_beaconInRepo_cancelsAndSendsMessage() {
        PlayerMock player = server.addPlayer();
        Block block = mock(Block.class);
        Location loc = new Location(world, 5, 64, 5);
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);
        when(block.getType()).thenReturn(Material.BEACON);
        when(block.getLocation()).thenReturn(loc);
        when(beaconSpawnService.findByLocation(loc)).thenReturn(Optional.of(beacon));
        BlockBreakEvent event = new BlockBreakEvent(block, player);

        listener.onBlockBreak(event);

        assertTrue(event.isCancelled());
    }

    // -------------------------------------------------------------------------
    // onBlockExplode
    // -------------------------------------------------------------------------

    @Test
    void onBlockExplode_removesSpawnedBeaconBlocks() {
        Block nonBeaconBlock = mock(Block.class);
        Block beaconBlock = mock(Block.class);
        Location beaconLoc = new Location(world, 0, 64, 0);
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);

        when(nonBeaconBlock.getType()).thenReturn(Material.STONE);
        when(beaconBlock.getType()).thenReturn(Material.BEACON);
        when(beaconBlock.getLocation()).thenReturn(beaconLoc);
        when(beaconSpawnService.findByLocation(beaconLoc)).thenReturn(Optional.of(beacon));

        ArrayList<Block> blocks = new ArrayList<>(List.of(nonBeaconBlock, beaconBlock));
        BlockExplodeEvent event = mock(BlockExplodeEvent.class);
        when(event.blockList()).thenReturn(blocks);

        listener.onBlockExplode(event);

        assertTrue(blocks.contains(nonBeaconBlock));
        assertFalse(blocks.contains(beaconBlock));
    }

    @Test
    void onBlockExplode_nonBeaconBlocks_notRemoved() {
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(Material.STONE);
        ArrayList<Block> blocks = new ArrayList<>(List.of(block));
        BlockExplodeEvent event = mock(BlockExplodeEvent.class);
        when(event.blockList()).thenReturn(blocks);

        listener.onBlockExplode(event);

        assertTrue(blocks.contains(block));
    }

    // -------------------------------------------------------------------------
    // onEntityExplode
    // -------------------------------------------------------------------------

    @Test
    void onEntityExplode_removesSpawnedBeaconBlocks() {
        Block beaconBlock = mock(Block.class);
        Location beaconLoc = new Location(world, 0, 64, 0);
        SpawnedBeacon beacon = mock(SpawnedBeacon.class);

        when(beaconBlock.getType()).thenReturn(Material.BEACON);
        when(beaconBlock.getLocation()).thenReturn(beaconLoc);
        when(beaconSpawnService.findByLocation(beaconLoc)).thenReturn(Optional.of(beacon));

        ArrayList<Block> blocks = new ArrayList<>(List.of(beaconBlock));
        EntityExplodeEvent event = mock(EntityExplodeEvent.class);
        when(event.blockList()).thenReturn(blocks);

        listener.onEntityExplode(event);

        assertFalse(blocks.contains(beaconBlock));
    }
}
