package com.skyblockexp.ezlifesteal.config;

import java.util.List;

/**
 * Configuration for the plugin-spawned revive beacon feature.
 *
 * <p>Beacons progress through {@code COUNTDOWN → AVAILABLE → USED/EXPIRED}.
 * Each nested record groups a distinct aspect of the feature.</p>
 */
public record BeaconSpawnSettings(
        boolean enabled,
        int maxConcurrent,
        WorldGuardSettings worldGuard,
        CountdownSettings countdown,
        RandomSpawnSettings randomSpawn,
        ScheduleSettings schedule,
        ExpirySettings expiry,
        AvailabilityEventSettings availabilityEvent
) {

    /** Returns a fully-disabled default (used when the feature is absent in config). */
    public static BeaconSpawnSettings disabled() {
        return new BeaconSpawnSettings(
                false,
                1,
                WorldGuardSettings.defaults(),
                CountdownSettings.defaults(),
                RandomSpawnSettings.defaults(),
                ScheduleSettings.defaults(),
                ExpirySettings.defaults(),
                AvailabilityEventSettings.defaults()
        );
    }

    /**
     * WorldGuard region protection settings created around each spawned beacon.
     */
    public record WorldGuardSettings(
            boolean enabled,
            int radius,
            boolean denyBuild,
            boolean denyPvp,
            boolean denyMobDamage,
            boolean denyExplosions
    ) {
        public static WorldGuardSettings defaults() {
            return new WorldGuardSettings(true, 10, true, false, false, false);
        }
    }

    /**
     * EzCountdown (or internal fallback) countdown shown while the beacon is warming up.
     */
    public record CountdownSettings(
            boolean enabled,
            int durationSeconds,
            List<String> displayTypes,
            String formatMessage,
            String bossBarColor,
            String bossBarStyle
    ) {
        public static CountdownSettings defaults() {
            return new CountdownSettings(
                    true,
                    300,
                    List.of("ACTION_BAR", "BOSS_BAR"),
                    "&5\u2620 &d&lRevive Beacon &5\u2620 &r&7 {formatted} until active",
                    "PURPLE",
                    "NOTCHED_20"
            );
        }
    }

    /**
     * Random world-spawn bounds used when no explicit location is provided.
     */
    public record RandomSpawnSettings(
            boolean enabled,
            String worldName,
            int minX,
            int maxX,
            int minZ,
            int maxZ
    ) {
        public static RandomSpawnSettings defaults() {
            return new RandomSpawnSettings(false, "world", -1000, 1000, -1000, 1000);
        }
    }

    /**
     * Automatic recurring spawn schedule.
     */
    public record ScheduleSettings(
            boolean enabled,
            int intervalMinutes
    ) {
        public static ScheduleSettings defaults() {
            return new ScheduleSettings(false, 60);
        }
    }

    /**
     * How long the beacon remains available before it is automatically removed.
     * {@code expiryMinutes == 0} means "never expire".
     */
    public record ExpirySettings(int expiryMinutes) {
        public static ExpirySettings defaults() {
            return new ExpirySettings(30);
        }
    }

    /**
     * The "cool event" broadcast when the beacon transitions to AVAILABLE status.
     */
    public record AvailabilityEventSettings(
            boolean broadcastEnabled,
            String broadcastMessageKey,
            boolean titleEnabled,
            String titleKey,
            String subtitleKey,
            boolean particlesEnabled,
            boolean fireworksEnabled
    ) {
        public static AvailabilityEventSettings defaults() {
            return new AvailabilityEventSettings(
                    true,
                    "beacon-spawn-available-broadcast",
                    true,
                    "beacon-spawn-available-title",
                    "beacon-spawn-available-subtitle",
                    true,
                    true
            );
        }
    }
}
