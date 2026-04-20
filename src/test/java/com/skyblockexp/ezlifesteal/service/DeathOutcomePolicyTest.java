package com.skyblockexp.ezlifesteal.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeathOutcomePolicyTest {

    private final DeathOutcomePolicy policy = new DeathOutcomePolicy();

    @Test
    void computeVictimOutcomeSkipsMobHeartLossWhenConfigured() {
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                false,
                true,
                true,
                -1,
                6.0,
                2.0,
                0.0,
                true
        ));

        assertFalse(outcome.applyHeartLoss());
        assertEquals(6.0, outcome.resultingHearts());
        assertFalse(outcome.shouldBan());
    }

    @Test
    void computeVictimOutcomeBansWhenHeartsReachZero() {
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                false,
                false,
                -1,
                1.0,
                2.0,
                0.0,
                true
        ));

        assertTrue(outcome.applyHeartLoss());
        assertEquals(-1.0, outcome.heartDelta());
        assertEquals(0.0, outcome.resultingHearts());
        assertTrue(outcome.shouldBan());
        assertFalse(outcome.shouldExecuteZeroHeartCommands());
    }

    @Test
    void computeKillerOutcomeSelectsHeartItemMode() {
        KillerOutcome outcome = policy.computeKillerOutcome(new DeathOutcomePolicy.KillerInput(
                false,
                false,
                true,
                1.5
        ));

        assertTrue(outcome.applyHeartGain());
        assertEquals(KillerRewardMode.HEART_ITEM, outcome.rewardMode());
        assertEquals(1.5, outcome.numericHeartGain());
    }
}
