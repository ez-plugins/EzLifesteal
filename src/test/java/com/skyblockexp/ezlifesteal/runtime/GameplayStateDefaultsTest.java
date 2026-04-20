package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameplayStateDefaultsTest {

    @Test
    void defaultsRemainStableAfterDomainSplit() {
        GameplayState defaults = GameplayState.defaults();

        assertEquals(0.0, defaults.getHeartRulesState().getHeartsPerKill());
        assertEquals(0.0, defaults.getHeartRulesState().getHeartsLostOnDeath());
        assertFalse(defaults.getHeartRulesState().isGlobalLifestealEnabled());
        assertFalse(defaults.getHeartRulesState().isBanWhenZeroHearts());

        assertFalse(defaults.getDropRulesState().isDropHeartOnKill());
        assertFalse(defaults.getDropRulesState().isDropHeartOnDeath());
        assertEquals(0, defaults.getDropRulesState().getDropHeartAmount());

        assertFalse(defaults.getReviveBeaconState().isReviveBeaconEnabled());
        assertEquals("beacon-revive-broadcast-hold-start",
                defaults.getReviveBeaconState().getReviveBeaconBroadcastHoldStartMessageKey());
        assertEquals("beacon-revive-broadcast-complete",
                defaults.getReviveBeaconState().getReviveBeaconBroadcastCompleteMessageKey());
        assertTrue(defaults.getReviveBeaconState().getReviveBeaconWhitelistedBeacons().isEmpty());

        assertTrue(defaults.getWorldRulesState().getEnabledWorlds().isEmpty());
        assertTrue(defaults.getWorldRulesState().getDisabledWorlds().isEmpty());
        assertTrue(defaults.getMobRulesState().getMobRewards().isEmpty());
        assertNotNull(defaults.getKillStreakSettings());
    }
}
