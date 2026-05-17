package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    void createParsesTeamKillBypassNestedSection() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.team-kill-bypass.enabled", true);
        root.set("lifesteal.team-kill-bypass.exempt-worlds", List.of("lobby", "spawn"));
        root.set("lifesteal.team-kill-bypass.min-team-size", 3);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.teamKillBypassWithTeamsApi());
        assertEquals(List.of("lobby", "spawn"), settings.teamKillBypassExemptWorlds());
        assertEquals(3, settings.teamKillBypassMinTeamSize());
    }

    @Test
    void createParsesTeamBankPerTeamOverrides() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.team-bank.enabled", true);
        root.set("lifesteal.team-bank.max-hearts", 500.0);
        root.set("lifesteal.team-bank.per-team-overrides.alpha", 100.0);
        root.set("lifesteal.team-bank.per-team-overrides.beta", 200.0);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.teamBankEnabled());
        assertEquals(100.0, settings.teamBankPerTeamMaxHearts().get("alpha"));
        assertEquals(200.0, settings.teamBankPerTeamMaxHearts().get("beta"));
    }

    @Test
    void createParsesBeaconSpawnSettingsWhenEnabled() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.revive-beacon.spawn.enabled", true);
        root.set("lifesteal.revive-beacon.spawn.max-concurrent", 2);
        root.set("lifesteal.revive-beacon.spawn.expiry-minutes", 15);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.beaconSpawnSettings().enabled());
        assertEquals(2, settings.beaconSpawnSettings().maxConcurrent());
        assertEquals(15, settings.beaconSpawnSettings().expiry().expiryMinutes());
    }

    @Test
    void createAddsWarningForNegativeBeaconSpawnExpiryMinutes() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.revive-beacon.spawn.enabled", true);
        root.set("lifesteal.revive-beacon.spawn.expiry-minutes", -5);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.beaconSpawnSettings().enabled());
        assertEquals(0, settings.beaconSpawnSettings().expiry().expiryMinutes());
        assertTrue(settings.warnings().stream().anyMatch(w -> w.contains("expiry-minutes")));
    }

    @Test
    void createAddsWarningForNegativeVoucherHoldSeconds() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.revive-beacon.voucher-hold-seconds", -3.0);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertEquals(0.0, settings.reviveBeaconVoucherHoldSeconds());
        assertTrue(settings.warnings().stream().anyMatch(w -> w.contains("voucher-hold-seconds")));
    }

    @Test
    void createFallsBackToLegacyPathForWhitelistedBeacons() {
        YamlConfiguration root = new YamlConfiguration();
        // revive-beacon.whitelisted-beacons is empty, but beacon-revive.whitelisted-beacons is set
        root.set("lifesteal.beacon-revive.whitelisted-beacons", List.of("world;10;64;10"));

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertEquals(List.of("world;10;64;10"), settings.reviveBeaconWhitelistedBeacons());
    }

    @Test
    void createAddsWarningForInvalidAnimationRingCount() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.revive-beacon.animation.ring-count", 0);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.reviveAnimationSettings().ringCount() > 0);
        assertTrue(settings.warnings().stream().anyMatch(w -> w.contains("ring-count")));
    }

    @Test
    void createUsesTeamKillBypassFlatKeyWhenSectionAbsent() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.team-kill-bypass-with-teams-api", true);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertTrue(settings.teamKillBypassWithTeamsApi());
        assertTrue(settings.teamKillBypassExemptWorlds().isEmpty());
        assertEquals(1, settings.teamKillBypassMinTeamSize());
    }

    @Test
    void createMinTeamSizeClampsToOneWhenZeroConfigured() {
        YamlConfiguration root = new YamlConfiguration();
        root.set("lifesteal.team-kill-bypass.enabled", true);
        root.set("lifesteal.team-kill-bypass.min-team-size", 0);

        LifestealConfigAdapter adapter = new LifestealConfigAdapter(null, root);
        LifestealSettingsFactory.LifestealSettings settings = new LifestealSettingsFactory().create(adapter);

        assertEquals(1, settings.teamKillBypassMinTeamSize());
    }
}
