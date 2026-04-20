package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifestealSettingsFactoryTest {

    @Test
    void createSanitizesHeartBoundsAndDropAmount() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.min-hearts", -5.0D);
        root.set("lifesteal.default-hearts", 50.0D);
        root.set("lifesteal.max-hearts", 40.0D);
        root.set("lifesteal.drop-heart-amount", 0);
        root.set("lifesteal.drop-heart-on-kill", true);
        root.set("lifesteal.revive-beacon.enabled", true);
        root.set("lifesteal.revive-beacon.voucher-heart-id", "basic");
        root.set("lifesteal.revive-beacon.require-sneak", true);
        root.set("lifesteal.revive-beacon.max-distance", -1.0D);
        root.set("lifesteal.revive-beacon.consume-on-fail", true);
        root.set("lifesteal.revive-beacon.require-voucher-in-beacon", true);
        root.set("lifesteal.revive-beacon.voucher-hold-seconds", 5.0D);
        root.set("lifesteal.revive-beacon.whitelist-enabled", true);
        root.set("lifesteal.revive-beacon.whitelisted-beacons", java.util.List.of("world;1;2;3"));
        root.set("lifesteal.revive-beacon.broadcast.enabled", true);
        root.set("lifesteal.revive-beacon.broadcast.hold-start-message-key", "custom-hold-key");
        root.set("lifesteal.revive-beacon.broadcast.complete-message-key", "custom-complete-key");
        root.set("lifesteal.revive-beacon.animation.duration-ticks", -2);
        root.set("lifesteal.revive-beacon.animation.spiral-steps", 12);
        root.set("lifesteal.revive-beacon.animation.vertical-lift-per-step", -1.0D);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory factory = new LifestealSettingsFactory();

        LifestealSettingsFactory.LifestealSettings settings = factory.create(adapter);

        assertEquals(0.0D, settings.minHearts());
        assertEquals(40.0D, settings.defaultHearts());
        assertEquals(40.0D, settings.maxHearts());
        assertEquals(1, settings.dropHeartAmount());
        assertTrue(settings.dropHeartOnDeath());
        assertTrue(settings.reviveBeaconEnabled());
        assertEquals("basic", settings.reviveBeaconVoucherHeartId());
        assertTrue(settings.reviveBeaconRequireSneak());
        assertEquals(8.0D, settings.reviveBeaconMaxDistance());
        assertTrue(settings.reviveBeaconConsumeOnFail());
        assertTrue(settings.reviveBeaconRequireVoucherInBeacon());
        assertEquals(5.0D, settings.reviveBeaconVoucherHoldSeconds());
        assertTrue(settings.reviveBeaconWhitelistEnabled());
        assertEquals(java.util.List.of("world;1;2;3"), settings.reviveBeaconWhitelistedBeacons());
        assertTrue(settings.reviveBeaconBroadcastEnabled());
        assertEquals("custom-hold-key", settings.reviveBeaconBroadcastHoldStartMessageKey());
        assertEquals("custom-complete-key", settings.reviveBeaconBroadcastCompleteMessageKey());
        assertEquals(30, settings.reviveAnimationSettings().durationTicks());
        assertEquals(12, settings.reviveAnimationSettings().spiralSteps());
        assertEquals(0.08D, settings.reviveAnimationSettings().verticalLiftPerStep());
        assertTrue(settings.heartBoundsAdjusted());
        assertEquals(4, settings.warnings().size());
    }

    @Test
    void createUsesReviveAnimationDefaultsWhenSectionMissing() {
        LifestealConfigAdapter adapter = new LifestealConfigAdapter(new YamlConfiguration(), new YamlConfiguration());
        LifestealSettingsFactory factory = new LifestealSettingsFactory();

        LifestealSettingsFactory.LifestealSettings settings = factory.create(adapter);

        assertTrue(settings.reviveAnimationSettings().enabled());
        assertEquals("END_ROD", settings.reviveAnimationSettings().spiralParticle().type());
        assertEquals("BLOCK_BEACON_ACTIVATE", settings.reviveAnimationSettings().impactSound().type());
    }
}
