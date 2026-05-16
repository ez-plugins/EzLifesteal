package com.skyblockexp.ezlifesteal.api.event;

import com.skyblockexp.ezlifesteal.model.SpawnedBeacon;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired on the main thread when a plugin-managed beacon is about to be registered.
 *
 * <p>The beacon block has already been placed and the {@link SpawnedBeacon} object has been
 * constructed, but the beacon has not yet been added to the active registry. Cancelling this
 * event will prevent the beacon from being tracked, remove the block, and return an empty
 * result from the spawn call.</p>
 */
public final class BeaconSpawnEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final SpawnedBeacon beacon;
    private boolean cancelled;

    public BeaconSpawnEvent(SpawnedBeacon beacon) {
        this.beacon = beacon;
    }

    /** Returns the beacon that is about to be registered. */
    public SpawnedBeacon getBeacon() {
        return beacon;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
