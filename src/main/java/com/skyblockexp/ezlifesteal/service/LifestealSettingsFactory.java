package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads, sanitizes, and exposes immutable lifesteal gameplay settings.
 */
public class LifestealSettingsFactory {
    private static final String REVIVE_BEACON_PATH = "revive-beacon";

    private static final String LEGACY_BEACON_REVIVE_PATH = "beacon-revive";


    private static final String DEFAULT_REVIVE_BEACON_VOUCHER_HEART_ID = "basic";

    private static final double DEFAULT_REVIVE_BEACON_MAX_DISTANCE = 8.0D;


    private static final boolean DEFAULT_REVIVE_BEACON_ENABLED = false;

    private static final boolean DEFAULT_REVIVE_BEACON_REQUIRE_SNEAK = false;

    private static final boolean DEFAULT_REVIVE_BEACON_CONSUME_ON_FAIL = false;

    private static final boolean DEFAULT_REVIVE_BEACON_REQUIRE_VOUCHER_IN_BEACON = false;

    private static final double DEFAULT_REVIVE_BEACON_VOUCHER_HOLD_SECONDS = 0.0D;

    private static final boolean DEFAULT_REVIVE_BEACON_WHITELIST_ENABLED = false;

    private static final boolean DEFAULT_REVIVE_BEACON_BROADCAST_ENABLED = false;

    private static final String DEFAULT_REVIVE_BEACON_BROADCAST_HOLD_START_MESSAGE_KEY =

            "beacon-revive-broadcast-hold-start";

    private static final String DEFAULT_REVIVE_BEACON_BROADCAST_COMPLETE_MESSAGE_KEY =

            "beacon-revive-broadcast-complete";

    public LifestealSettings create(LifestealConfigAdapter lifestealConfigAdapter) {
        final double configuredDefaultHearts = lifestealConfigAdapter.getDouble("default-hearts", 10.0);
        final double configuredMinHearts = lifestealConfigAdapter.getDouble("min-hearts", 1.0);
        final double configuredMaxHearts = lifestealConfigAdapter.getDouble("max-hearts", 40.0);
        final DefaultPluginRuntimeServices.SanitizedHeartBounds heartBounds =
                DefaultPluginRuntimeServices.sanitizeHeartBounds(
                configuredMinHearts,
                configuredDefaultHearts,
                configuredMaxHearts
        );

        final List<String> warnings = new ArrayList<>();
        if (heartBounds.adjusted()) {
            warnings.add(String.format(
                    "Adjusted lifesteal heart bounds from min=%.2f/default=%.2f/max=%.2f to"
                            + " min=%.2f/default=%.2f/max=%.2f",
                    configuredMinHearts,
                    configuredDefaultHearts,
                    configuredMaxHearts,
                    heartBounds.minHearts(),
                    heartBounds.defaultHearts(),
                    heartBounds.maxHearts()
            ));
        }

        final boolean dropHeartOnDeath = lifestealConfigAdapter.getBoolean(
                "drop-heart-on-death",
                lifestealConfigAdapter.getBoolean("drop-heart-on-kill", false)
        );
        final boolean reviveBeaconEnabled = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "enabled",
                DEFAULT_REVIVE_BEACON_ENABLED
        );
        final String reviveBeaconVoucherHeartId = getStringWithLegacyFallback(
                lifestealConfigAdapter,
                "voucher-heart-id",
                DEFAULT_REVIVE_BEACON_VOUCHER_HEART_ID
        );
        final boolean reviveBeaconRequireSneak = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "require-sneak",
                DEFAULT_REVIVE_BEACON_REQUIRE_SNEAK
        );
        double reviveBeaconMaxDistance = getDoubleWithLegacyFallback(
                lifestealConfigAdapter,
                "max-distance",
                DEFAULT_REVIVE_BEACON_MAX_DISTANCE
        );
        if (reviveBeaconMaxDistance <= 0.0D) {
            warnings.add(String.format(
                    "Invalid revive beacon max-distance %.2f configured; using default %.2f",
                    reviveBeaconMaxDistance,
                    DEFAULT_REVIVE_BEACON_MAX_DISTANCE
            ));
            reviveBeaconMaxDistance = DEFAULT_REVIVE_BEACON_MAX_DISTANCE;
        }
        final boolean reviveBeaconConsumeOnFail = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "consume-on-fail",
                DEFAULT_REVIVE_BEACON_CONSUME_ON_FAIL
        );
        final boolean reviveBeaconRequireVoucherInBeacon = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "require-voucher-in-beacon",
                DEFAULT_REVIVE_BEACON_REQUIRE_VOUCHER_IN_BEACON
        );
        double reviveBeaconVoucherHoldSeconds = getDoubleWithLegacyFallback(
                lifestealConfigAdapter,
                "voucher-hold-seconds",
                DEFAULT_REVIVE_BEACON_VOUCHER_HOLD_SECONDS
        );
        if (reviveBeaconVoucherHoldSeconds < 0.0D) {
            warnings.add(String.format(
                    "Invalid revive beacon voucher-hold-seconds %.2f configured; using default %.2f",
                    reviveBeaconVoucherHoldSeconds,
                    DEFAULT_REVIVE_BEACON_VOUCHER_HOLD_SECONDS
            ));
            reviveBeaconVoucherHoldSeconds = DEFAULT_REVIVE_BEACON_VOUCHER_HOLD_SECONDS;
        }
        final boolean reviveBeaconWhitelistEnabled = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "whitelist-enabled",
                DEFAULT_REVIVE_BEACON_WHITELIST_ENABLED
        );
        final List<String> reviveBeaconWhitelistedBeacons = List.copyOf(getStringListWithLegacyFallback(
                lifestealConfigAdapter,
                "whitelisted-beacons"
        ));
        final ReviveAnimationSettings reviveAnimationSettings = parseReviveAnimationSettings(lifestealConfigAdapter,
                warnings);
        final boolean reviveBeaconBroadcastEnabled = getBooleanWithLegacyFallback(
                lifestealConfigAdapter,
                "broadcast.enabled",
                DEFAULT_REVIVE_BEACON_BROADCAST_ENABLED
        );
        final String reviveBeaconBroadcastHoldStartMessageKey = getStringWithLegacyFallback(
                lifestealConfigAdapter,
                "broadcast.hold-start-message-key",
                DEFAULT_REVIVE_BEACON_BROADCAST_HOLD_START_MESSAGE_KEY
        );
        final String reviveBeaconBroadcastCompleteMessageKey = getStringWithLegacyFallback(
                lifestealConfigAdapter,
                "broadcast.complete-message-key",
                DEFAULT_REVIVE_BEACON_BROADCAST_COMPLETE_MESSAGE_KEY
        );

        return new LifestealSettings(
                heartBounds.defaultHearts(),
                heartBounds.minHearts(),
                heartBounds.maxHearts(),
                lifestealConfigAdapter.getBoolean("apply-health-scale", false),
                lifestealConfigAdapter.getDouble("health-scale", 20.0),
                lifestealConfigAdapter.getDouble("hearts-per-kill", 1.0),
                lifestealConfigAdapter.getDouble("hearts-lost-on-death", 1.0),
                lifestealConfigAdapter.getBoolean("dont-remove-hearts-from-mobs", true),
                lifestealConfigAdapter.getDouble("mob-remove-hearts-greater-than", -1.0),
                dropHeartOnDeath,
                lifestealConfigAdapter.getBoolean("drop-heart-only-when-killed-by-player", true),
                dropHeartOnDeath,
                lifestealConfigAdapter.getString("drop-heart-id", "basic"),
                Math.max(1, lifestealConfigAdapter.getInt("drop-heart-amount", 1)),
                reviveBeaconEnabled,
                reviveBeaconVoucherHeartId,
                reviveBeaconRequireSneak,
                reviveBeaconMaxDistance,
                reviveBeaconConsumeOnFail,
                reviveBeaconRequireVoucherInBeacon,
                reviveBeaconVoucherHoldSeconds,
                reviveBeaconWhitelistEnabled,
                reviveBeaconWhitelistedBeacons,
                reviveAnimationSettings,
                reviveBeaconBroadcastEnabled,
                reviveBeaconBroadcastHoldStartMessageKey,
                reviveBeaconBroadcastCompleteMessageKey,
                lifestealConfigAdapter.getBoolean("ban-when-zero-hearts", false),
                List.copyOf(lifestealConfigAdapter.getStringList("zero-heart-commands")),
                lifestealConfigAdapter.getBoolean("global-enabled", true),
                configuredMinHearts,
                configuredDefaultHearts,
                configuredMaxHearts,
                heartBounds.adjusted(),
                warnings
        );
    }

    private boolean getBooleanWithLegacyFallback(LifestealConfigAdapter adapter, String childPath,
            boolean defaultValue) {
        return adapter.getBoolean(
                REVIVE_BEACON_PATH + "." + childPath,
                adapter.getBoolean(LEGACY_BEACON_REVIVE_PATH + "." + childPath, defaultValue)
        );
    }

    private String getStringWithLegacyFallback(LifestealConfigAdapter adapter, String childPath, String defaultValue) {
        return adapter.getString(
                REVIVE_BEACON_PATH + "." + childPath,
                adapter.getString(LEGACY_BEACON_REVIVE_PATH + "." + childPath, defaultValue)
        );
    }

    private double getDoubleWithLegacyFallback(LifestealConfigAdapter adapter, String childPath, double defaultValue) {
        return adapter.getDouble(
                REVIVE_BEACON_PATH + "." + childPath,
                adapter.getDouble(LEGACY_BEACON_REVIVE_PATH + "." + childPath, defaultValue)
        );
    }

    private List<String> getStringListWithLegacyFallback(LifestealConfigAdapter adapter, String childPath) {
        final List<String> primary = adapter.getStringList(REVIVE_BEACON_PATH + "." + childPath);
        if (!primary.isEmpty()) {
            return primary;
        }
        return adapter.getStringList(LEGACY_BEACON_REVIVE_PATH + "." + childPath);
    }

    private ReviveAnimationSettings parseReviveAnimationSettings(LifestealConfigAdapter adapter,
            List<String> warnings) {
        final ReviveAnimationSettings defaults = ReviveAnimationSettings.defaults();
        final String animationPath = REVIVE_BEACON_PATH + ".animation";
        final boolean enabled = adapter.getBoolean(animationPath + ".enabled", defaults.enabled());
        final int durationTicks = sanitizePositiveInt(
                adapter.getInt(animationPath + ".duration-ticks", defaults.durationTicks()),
                defaults.durationTicks(),
                animationPath + ".duration-ticks",
                warnings
        );
        final int spiralSteps = sanitizePositiveInt(
                adapter.getInt(animationPath + ".spiral-steps", defaults.spiralSteps()),
                defaults.spiralSteps(),
                animationPath + ".spiral-steps",
                warnings
        );
        final int ringCount = sanitizePositiveInt(
                adapter.getInt(animationPath + ".ring-count", defaults.ringCount()),
                defaults.ringCount(),
                animationPath + ".ring-count",
                warnings
        );
        final double verticalLiftPerStep = sanitizePositiveDouble(
                adapter.getDouble(animationPath + ".vertical-lift-per-step", defaults.verticalLiftPerStep()),
                defaults.verticalLiftPerStep(),
                animationPath + ".vertical-lift-per-step",
                warnings
        );
        final ReviveAnimationSettings.ParticlePreset spiral = parseParticlePreset(
                adapter,
                animationPath + ".particles.spiral",
                defaults.spiralParticle()
        );
        final ReviveAnimationSettings.ParticlePreset ring = parseParticlePreset(
                adapter,
                animationPath + ".particles.ring",
                defaults.ringParticle()
        );
        final ReviveAnimationSettings.ParticlePreset impact = parseParticlePreset(
                adapter,
                animationPath + ".particles.impact",
                defaults.impactParticle()
        );
        final ReviveAnimationSettings.SoundPreset loopSound = parseSoundPreset(
                adapter,
                animationPath + ".sounds.loop",
                defaults.loopSound()
        );
        final ReviveAnimationSettings.SoundPreset impactSound = parseSoundPreset(
                adapter,
                animationPath + ".sounds.impact",
                defaults.impactSound()
        );
        return new ReviveAnimationSettings(
                enabled,
                durationTicks,
                spiralSteps,
                ringCount,
                verticalLiftPerStep,
                spiral,
                ring,
                impact,
                loopSound,
                impactSound
        );
    }

    private int sanitizePositiveInt(int value, int defaultValue, String path, List<String> warnings) {
        if (value > 0) {
            return value;
        }
        warnings.add("Invalid " + path + " " + value + " configured; using default " + defaultValue);
        return defaultValue;
    }

    private double sanitizePositiveDouble(double value, double defaultValue, String path, List<String> warnings) {
        if (value > 0.0D) {
            return value;
        }
        warnings.add(String.format("Invalid %s %.2f configured; using default %.2f", path, value, defaultValue));
        return defaultValue;
    }

    private ReviveAnimationSettings.ParticlePreset parseParticlePreset(
            LifestealConfigAdapter adapter,
            String path,
            ReviveAnimationSettings.ParticlePreset defaults
    ) {
        return new ReviveAnimationSettings.ParticlePreset(
                adapter.getString(path + ".type", defaults.type()),
                Math.max(1, adapter.getInt(path + ".count", defaults.count())),
                adapter.getDouble(path + ".offset-x", defaults.offsetX()),
                adapter.getDouble(path + ".offset-y", defaults.offsetY()),
                adapter.getDouble(path + ".offset-z", defaults.offsetZ()),
                adapter.getDouble(path + ".speed", defaults.speed())
        );
    }

    private ReviveAnimationSettings.SoundPreset parseSoundPreset(
            LifestealConfigAdapter adapter,
            String path,
            ReviveAnimationSettings.SoundPreset defaults
    ) {
        return new ReviveAnimationSettings.SoundPreset(
                adapter.getString(path + ".type", defaults.type()),
                (float) adapter.getDouble(path + ".volume", defaults.volume()),
                (float) adapter.getDouble(path + ".pitch", defaults.pitch())
        );
    }

    public record LifestealSettings(
            double defaultHearts,
            double minHearts,
            double maxHearts,
            boolean applyHealthScale,
            double healthScale,
            double heartsPerKill,
            double heartsLostOnDeath,
            boolean dontRemoveHeartsFromMobs,
            double mobRemoveHeartsGreaterThan,
            boolean dropHeartOnDeath,
            boolean dropHeartOnlyWhenKilledByPlayer,
            boolean dropHeartOnKill,
            String dropHeartId,
            int dropHeartAmount,
            boolean reviveBeaconEnabled,
            String reviveBeaconVoucherHeartId,
            boolean reviveBeaconRequireSneak,
            double reviveBeaconMaxDistance,
            boolean reviveBeaconConsumeOnFail,
            boolean reviveBeaconRequireVoucherInBeacon,
            double reviveBeaconVoucherHoldSeconds,
            boolean reviveBeaconWhitelistEnabled,
            List<String> reviveBeaconWhitelistedBeacons,
            ReviveAnimationSettings reviveAnimationSettings,
            boolean reviveBeaconBroadcastEnabled,
            String reviveBeaconBroadcastHoldStartMessageKey,
            String reviveBeaconBroadcastCompleteMessageKey,
            boolean banWhenZeroHearts,
            List<String> zeroHeartCommands,
            boolean globalLifestealEnabled,
            double configuredMinHearts,
            double configuredDefaultHearts,
            double configuredMaxHearts,
            boolean heartBoundsAdjusted,
            List<String> warnings
    ) {
    }
}
