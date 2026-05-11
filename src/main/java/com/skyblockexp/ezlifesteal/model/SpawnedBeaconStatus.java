package com.skyblockexp.ezlifesteal.model;

/** Lifecycle states for a plugin-spawned revive beacon. */
public enum SpawnedBeaconStatus {

    /** Beacon is in the world but the countdown to availability has not yet finished. */
    COUNTDOWN,

    /** Countdown finished; beacon is open for use. */
    AVAILABLE,

    /** Beacon was successfully used to revive a player. */
    USED,

    /** Beacon expired before it was used. */
    EXPIRED
}
