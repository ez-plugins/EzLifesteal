package com.skyblockexp.ezlifesteal.api.event;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a beacon transitions to the AVAILABLE state.
 *
 * <p>This event fires after the beacon countdown ends (or immediately if no countdown is
 * configured). At this point the beacon is open for player interaction.</p>
 */
public final class BeaconAvailableEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpawnedBeacon beacon;

    public BeaconAvailableEvent(SpawnedBeacon beacon) {
        this.beacon = beacon;
    }

    /** Returns the beacon that became available. */
    public SpawnedBeacon getBeacon() {
        return beacon;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
