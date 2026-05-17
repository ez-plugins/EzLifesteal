package com.skyblockexp.ezlifesteal.api.event;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a beacon is consumed by a successful player revive.
 *
 * <p>This event fires after the beacon's status has been set to {@code USED} and its
 * block has been removed from the world.</p>
 */
public final class BeaconUsedEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpawnedBeacon beacon;

    public BeaconUsedEvent(SpawnedBeacon beacon) {
        this.beacon = beacon;
    }

    /** Returns the beacon that was used. */
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
