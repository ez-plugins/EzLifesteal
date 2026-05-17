package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayStateApplierTest {

    @Test
    void applyWritesValuesIntoSplitDomainStates() {
        GameplayState gameplayState = GameplayState.defaults();
        GameplayStateApplier applier = new GameplayStateApplier();

        LifestealSettingsFactory.LifestealSettings lifestealSettings = new LifestealSettingsFactory.LifestealSettings(
                10.0,
                1.0,
                20.0,
                false,
                20.0,
                2.5,
                1.5,
                true,
                List.of(),
                1,
                true,
                100.0,
                Map.of(),
                true,
                7.0,
                true,
                true,
                true,
                "legendary",
                3,
                true,
                "voucher",
                true,
                16.0,
                true,
                true,
                4.0,
                true,
                List.of("beacon_a"),
                ReviveAnimationSettings.defaults(),
                true,
                "hold-start",
                "complete",
                BeaconSpawnSettings.disabled(),
                true,
                List.of("say hi"),
                false,
                1.0,
                10.0,
                20.0,
                false,
                List.of()
        );
        AdminSettingsFactory.AdminSettings adminSettings = new AdminSettingsFactory.AdminSettings(
                null,
                true,
                false,
                false,
                List.of()
        );
        KillStreakSettings killStreakSettings = KillStreakSettings.disabled();

        applier.apply(
                gameplayState,
                lifestealSettings,
                adminSettings,
                killStreakSettings,
                Set.of("world"),
                Set.of("world_nether")
        );

        assertEquals(2.5, gameplayState.getHeartRulesState().getHeartsPerKill());
        assertEquals(1.5, gameplayState.getHeartRulesState().getHeartsLostOnDeath());
        assertTrue(gameplayState.getDropRulesState().isDropHeartOnDeath());
        assertEquals("legendary", gameplayState.getDropRulesState().getDropHeartId());
        assertTrue(gameplayState.getReviveBeaconState().isReviveBeaconEnabled());
        assertEquals("voucher", gameplayState.getReviveBeaconState().getReviveBeaconVoucherHeartId());
        assertEquals(16.0, gameplayState.getReviveBeaconState().getReviveBeaconMaxDistance());
        assertTrue(gameplayState.getMobRulesState().isDontRemoveHeartsFromMobs());
        assertEquals(Set.of("world"), gameplayState.getWorldRulesState().getEnabledWorlds());
        assertEquals(Set.of("world_nether"), gameplayState.getWorldRulesState().getDisabledWorlds());
        assertSame(killStreakSettings, gameplayState.getKillStreakSettings());
    }
}
