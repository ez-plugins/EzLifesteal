package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.MessageService;
import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.World;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BeaconAvailabilityServiceTest {

    private ServerMock server;
    private WorldMock world;
    private Logger logger;
    private BeaconAvailabilityService service;
    private PluginAccessor accessor;
    private MessageService messageService;

    @BeforeEach
    void setUp() {
        server = MockBukkit.mock();
        world = server.addSimpleWorld("testworld");
        logger = mock(Logger.class);
        service = new BeaconAvailabilityService(logger);

        accessor = mock(PluginAccessor.class);
        messageService = mock(MessageService.class);
        when(accessor.getMessageService()).thenReturn(messageService);
        when(accessor.getPlugin()).thenReturn(MockBukkit.createMockPlugin());
        when(messageService.format(anyString(), any())).thenReturn("");
        when(messageService.render(anyString(), any())).thenReturn("");
    }

    @AfterEach
    void tearDown() {
        if (MockBukkit.isMocked()) {
            MockBukkit.unmock();
        }
    }

    private SpawnedBeacon beaconAt(Location location) {
        return new SpawnedBeacon(
                UUID.randomUUID(),
                location,
                null,
                null,
                System.currentTimeMillis(),
                SpawnedBeaconStatus.AVAILABLE,
                0
        );
    }

    private BeaconSpawnSettings.AvailabilityEventSettings allDisabled() {
        return new BeaconSpawnSettings.AvailabilityEventSettings(
                false, "broadcast-key",
                false, "title-key", "subtitle-key",
                false, false
        );
    }

    private BeaconSpawnSettings.AvailabilityEventSettings allEnabled() {
        return new BeaconSpawnSettings.AvailabilityEventSettings(
                true, "broadcast-key",
                true, "title-key", "subtitle-key",
                true, true
        );
    }

    @Test
    void fireAvailabilityEvent_noWorld_logsWarning() {
        Location locationWithoutWorld = new Location(null, 0, 64, 0);
        SpawnedBeacon beacon = beaconAt(locationWithoutWorld);

        service.fireAvailabilityEvent(beacon, accessor, allDisabled());

        verify(logger).warning(anyString());
    }

    @Test
    void fireAvailabilityEvent_allDisabled_doesNotInteractWithMessageService() {
        Location loc = new Location(world, 0, 64, 0);
        SpawnedBeacon beacon = beaconAt(loc);

        service.fireAvailabilityEvent(beacon, accessor, allDisabled());

        verify(messageService, never()).format(anyString(), any());
        verify(messageService, never()).render(anyString(), any());
    }

    @Test
    void fireAvailabilityEvent_broadcastEnabled_formatsMessage() {
        Location loc = new Location(world, 10, 64, 10);
        SpawnedBeacon beacon = beaconAt(loc);
        BeaconSpawnSettings.AvailabilityEventSettings settings =
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        true, "broadcast-key",
                        false, "title-key", "subtitle-key",
                        false, false
                );
        when(messageService.format(anyString(), any())).thenReturn("Beacon is available!");

        service.fireAvailabilityEvent(beacon, accessor, settings);

        verify(messageService).format(anyString(), any());
    }

    @Test
    void fireAvailabilityEvent_broadcastEnabled_blankMessage_doesNotBroadcast() {
        Location loc = new Location(world, 20, 64, 20);
        SpawnedBeacon beacon = beaconAt(loc);
        BeaconSpawnSettings.AvailabilityEventSettings settings =
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        true, "broadcast-key",
                        false, "title-key", "subtitle-key",
                        false, false
                );
        when(messageService.format(anyString(), any())).thenReturn("");

        // Should not throw even if message is blank
        service.fireAvailabilityEvent(beacon, accessor, settings);
    }

    @Test
    void fireAvailabilityEvent_titleEnabled_callsRender() {
        Location loc = new Location(world, 30, 64, 30);
        SpawnedBeacon beacon = beaconAt(loc);
        server.addPlayer("player1");
        BeaconSpawnSettings.AvailabilityEventSettings settings =
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        false, "broadcast-key",
                        true, "title-key", "subtitle-key",
                        false, false
                );
        when(messageService.render(anyString(), any())).thenReturn("Title!");

        service.fireAvailabilityEvent(beacon, accessor, settings);

        verify(messageService).render("title-key", java.util.Map.of(
                "world", "testworld",
                "x", "30", "y", "64", "z", "30"
        ));
    }

    @Test
    void fireAvailabilityEvent_messageServiceNull_doesNotThrow() {
        when(accessor.getMessageService()).thenReturn(null);
        Location loc = new Location(world, 40, 64, 40);
        SpawnedBeacon beacon = beaconAt(loc);
        BeaconSpawnSettings.AvailabilityEventSettings settings =
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        true, "broadcast-key",
                        true, "title-key", "subtitle-key",
                        false, false
                );

        // Should not throw
        service.fireAvailabilityEvent(beacon, accessor, settings);
    }

    @Test
    void fireAvailabilityEvent_particlesEnabled_doesNotThrow() {
        Location loc = new Location(world, 50, 64, 50);
        SpawnedBeacon beacon = beaconAt(loc);
        BeaconSpawnSettings.AvailabilityEventSettings settings =
                new BeaconSpawnSettings.AvailabilityEventSettings(
                        false, "broadcast-key",
                        false, "title-key", "subtitle-key",
                        true, false
                );

        // Should not throw in a mock server environment
        service.fireAvailabilityEvent(beacon, accessor, settings);
    }
}
