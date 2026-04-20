package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.LifestealConfigAdapter;
import com.skyblockexp.ezlifesteal.model.MobReward;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameplayParsingServiceTest {

    @Test
    void parseWorldOverridesReturnsEmptyWhenSectionMissing() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        when(runtime.getLifestealConfigAdapter()).thenReturn(new LifestealConfigAdapter(new YamlConfiguration(), null));

        GameplayParsingService.WorldOverrides parsed = new GameplayParsingService(runtime).parseWorldOverrides();

        assertTrue(parsed.heartsPerKillOverrides().isEmpty());
        assertTrue(parsed.heartsLostOnDeathOverrides().isEmpty());
        assertTrue(parsed.banWhenZeroOverrides().isEmpty());
    }

    @Test
    void parseWorldOverridesSkipsInvalidEntriesAndParsesValidValues() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("world-overrides.bad-entry", "not-a-section");
        config.set("world-overrides.world_nether.hearts-per-kill", 1.5D);
        config.set("world-overrides.world_nether.hearts-lost-on-death", 2.0D);
        config.set("world-overrides.world_nether.ban-when-zero", true);
        when(runtime.getLogger()).thenReturn(logger);
        when(runtime.normalizeWorldName("world_nether")).thenReturn("world_nether");
        when(runtime.getLifestealConfigAdapter()).thenReturn(new LifestealConfigAdapter(config, null));

        GameplayParsingService.WorldOverrides parsed = new GameplayParsingService(runtime).parseWorldOverrides();

        assertEquals(Map.of("world_nether", 1.5D), parsed.heartsPerKillOverrides());
        assertEquals(Map.of("world_nether", 2.0D), parsed.heartsLostOnDeathOverrides());
        assertEquals(Map.of("world_nether", true), parsed.banWhenZeroOverrides());
        verify(logger).warning(contains("bad-entry"));
    }

    @Test
    void parseMobRewardsHandlesBlankInvalidAndPermissionNormalization() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Logger logger = mock(Logger.class);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mob-rewards.invalid", "not-a-section");
        config.set("mob-rewards.not_an_entity.hearts", 1.0D);
        config.set("mob-rewards.zombie.hearts", 2.5D);
        config.set("mob-rewards.zombie.worlds", java.util.List.of("world", " world_nether "));
        config.set("mob-rewards.zombie.blocked-worlds", java.util.List.of("world_the_end"));
        config.set("mob-rewards.zombie.permission", "   ");

        when(runtime.getLogger()).thenReturn(logger);
        when(runtime.getLifestealConfigAdapter()).thenReturn(new LifestealConfigAdapter(config, null));

        Map<EntityType, MobReward> parsed = new GameplayParsingService(runtime).parseMobRewards();

        assertEquals(1, parsed.size());
        MobReward reward = parsed.get(EntityType.ZOMBIE);
        assertEquals(2.5D, reward.getHearts());
        assertTrue(reward.isWorldAllowed("world"));
        assertTrue(!reward.isWorldAllowed("world_nether"));
        assertTrue(!reward.isWorldAllowed("world_the_end"));
        assertEquals(null, reward.getPermission());
        verify(logger).warning(contains("invalid"));
        verify(logger).warning(contains("not_an_entity"));
    }
}
