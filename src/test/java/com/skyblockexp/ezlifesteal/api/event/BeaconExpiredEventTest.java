package com.skyblockexp.ezlifesteal.api.event;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import com.skyblockexp.ezlifesteal.model.SpawnedBeaconStatus;
import java.util.UUID;
import org.bukkit.Location;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class BeaconExpiredEventTest {

    @Test
    void getBeacon_returnsBeaconProvidedAtConstruction() {
        SpawnedBeacon beacon = new SpawnedBeacon(
                UUID.randomUUID(),
                new Location(null, 0, 64, 0),
                null,
                null,
                System.currentTimeMillis(),
                SpawnedBeaconStatus.AVAILABLE,
                0L
        );
        BeaconExpiredEvent event = new BeaconExpiredEvent(beacon);
        assertSame(beacon, event.getBeacon());
    }

    @Test
    void getHandlers_returnsNonNull() {
        SpawnedBeacon beacon = new SpawnedBeacon(
                UUID.randomUUID(),
                new Location(null, 0, 64, 0),
                null,
                null,
                System.currentTimeMillis(),
                SpawnedBeaconStatus.AVAILABLE,
                0L
        );
        BeaconExpiredEvent event = new BeaconExpiredEvent(beacon);
        assertNotNull(event.getHandlers());
    }

    @Test
    void getHandlerList_returnsNonNull() {
        assertNotNull(BeaconExpiredEvent.getHandlerList());
    }

    @Test
    void getHandlerList_sameInstanceAsGetHandlers() {
        SpawnedBeacon beacon = new SpawnedBeacon(
                UUID.randomUUID(),
                new Location(null, 0, 64, 0),
                null,
                null,
                System.currentTimeMillis(),
                SpawnedBeaconStatus.AVAILABLE,
                0L
        );
        BeaconExpiredEvent event = new BeaconExpiredEvent(beacon);
        assertSame(BeaconExpiredEvent.getHandlerList(), event.getHandlers());
    }
}
