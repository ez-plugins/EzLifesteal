package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultPluginRuntimeServicesParsingTest {

    // Reward item parsing creates ItemStacks and can trigger server/registry initialization

    // in the test environment (MockBukkit). Avoid exercising ItemStack creation here.

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void parse_world_overrides_and_mob_rewards_and_killstreaks() throws Exception {
        EzLifestealPlugin plugin = MockBukkit.load(EzLifestealPlugin.class);
        Registry registry = new Registry();
        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);
        services.initializeCoreState();

        // build a lifesteal config with world-overrides, mob-rewards and kill-streaks
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("world-overrides.world1.hearts-per-kill", 2.0);
        cfg.set("world-overrides.world1.hearts-lost-on-death", 0.5);
        cfg.set("world-overrides.world1.ban-when-zero", true);

        cfg.set("mob-rewards.ZOMBIE.hearts", 2.5);
        cfg.set("mob-rewards.ZOMBIE.worlds", List.of("world1"));

        cfg.set("kill-streaks.enabled", true);
        cfg.set("kill-streaks.rewards.3.streak", 3);
        cfg.set("kill-streaks.rewards.3.money", 10.5);
        cfg.set("kill-streaks.rewards.3.commands", List.of("say hello"));
        // leave items empty to avoid ItemStack creation during tests

        // inject lifestealConfigAdapter using reflection
        var adapter = new com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter(cfg, List.of(),
                new YamlConfiguration());
        var field = DefaultPluginRuntimeServices.class.getDeclaredField("lifestealConfigAdapter");
        field.setAccessible(true);
        field.set(services, adapter);

        services.parseWorldOverrides();

        assertEquals(2.0, services.getHeartsPerKill("world1"));
        assertEquals(0.5, services.getHeartsLostOnDeath("world1"));
        assertTrue(services.isBanWhenZeroHearts("world1"));

        // parse mob rewards
        services.parseMobRewards();

        var reward = services.getMobReward(EntityType.ZOMBIE);
        assertNotNull(reward);
        assertEquals(2.5, reward.getHearts());

        // parse kill streaks
        Object ks = services.parseKillStreakSettings();
        assertNotNull(ks);
        assertTrue(((com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings) ks).enabled());
    }
}
