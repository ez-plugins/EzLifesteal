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
    void computeVictimOutcomeBansWhenHeartsReachZeroWithMinHeartsFloor() {
        // Reproduces the bug: default config has min-hearts: 1.0, which previously caused
        // resultingHearts to be clamped to 1.0, making shouldBan always false.
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                false,
                false,
                -1,
                1.0,   // victim has exactly 1 heart (the minimum)
                1.0,   // loses 1 heart on death -> raw result = 0
                1.0,   // min-hearts = 1.0 (default config value)
                true
        ));

        assertTrue(outcome.applyHeartLoss());
        // hearts stay clamped at minHearts (1.0), but the ban must still fire
        assertEquals(1.0, outcome.resultingHearts());
        assertTrue(outcome.shouldBan(), "Ban must fire even when minHearts floor clamps resultingHearts above 0");
        assertFalse(outcome.shouldExecuteZeroHeartCommands());
    }

    @Test
    void computeVictimOutcomeZeroHeartCommandsFireWithMinHeartsFloor() {
        // Same floor scenario, but banWhenZeroHearts = false → zero-heart commands should fire.
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                false,
                false,
                -1,
                1.0,
                1.0,
                1.0,
                false
        ));

        assertTrue(outcome.applyHeartLoss());
        assertFalse(outcome.shouldBan());
        assertTrue(outcome.shouldExecuteZeroHeartCommands(),
                "Zero-heart commands must fire even when minHearts floor clamps resultingHearts above 0");
    }

    @Test
    void computeVictimOutcomeNoBanWhenHeartsRemainingWithMinHeartsFloor() {
        // Player has 2 hearts, loses 1 → rawResult = 1.0 > 0 → no ban.
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                false,
                false,
                -1,
                2.0,
                1.0,
                1.0,
                true
        ));

        assertTrue(outcome.applyHeartLoss());
        assertEquals(1.0, outcome.resultingHearts());
        assertFalse(outcome.shouldBan());
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

    @Test
    void computeKillerOutcomeAdminBypassStopsHeartGain() {
        KillerOutcome outcome = policy.computeKillerOutcome(new DeathOutcomePolicy.KillerInput(
                true,
                true,
                false,
                2.0
        ));

        assertFalse(outcome.applyHeartGain());
        assertEquals(KillerRewardMode.NONE, outcome.rewardMode());
        assertEquals(0.0, outcome.numericHeartGain());
    }

    @Test
    void computeKillerOutcomeNumericModeWhenDropHeartDisabled() {
        KillerOutcome outcome = policy.computeKillerOutcome(new DeathOutcomePolicy.KillerInput(
                false,
                false,
                false,
                1.5
        ));

        assertTrue(outcome.applyHeartGain());
        assertEquals(KillerRewardMode.NUMERIC, outcome.rewardMode());
        assertEquals(1.5, outcome.numericHeartGain());
    }

    @Test
    void computeKillerOutcomeAdminWithoutBypassStillGetsReward() {
        KillerOutcome outcome = policy.computeKillerOutcome(new DeathOutcomePolicy.KillerInput(
                true,
                false,
                true,
                2.0
        ));

        assertTrue(outcome.applyHeartGain());
        assertEquals(KillerRewardMode.HEART_ITEM, outcome.rewardMode());
        assertEquals(2.0, outcome.numericHeartGain());
    }

    @Test
    void computeVictimOutcomeAdminBypassPreventsHeartLoss() {
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                true,
                true,
                true,
                false,
                false,
                -1,
                10.0,
                2.0,
                0.0,
                true
        ));

        assertFalse(outcome.applyHeartLoss());
        assertEquals(10.0, outcome.resultingHearts());
        assertFalse(outcome.shouldBan());
        assertFalse(outcome.shouldExecuteZeroHeartCommands());
    }

    @Test
    void computeVictimOutcomeAdminWithoutBypassLosesHearts() {
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                true,
                false,
                true,
                false,
                false,
                -1,
                10.0,
                2.0,
                0.0,
                false
        ));

        assertTrue(outcome.applyHeartLoss());
        assertEquals(8.0, outcome.resultingHearts());
        assertEquals(-2.0, outcome.heartDelta());
        assertFalse(outcome.shouldBan());
    }

    @Test
    void computeVictimOutcomeMobDeathAboveThresholdAppliesHeartLoss() {
        // mobRemoveHeartsGreaterThan = 5.0, victim has 6.0 → strictly greater → apply loss
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                false,
                true,
                false,
                5.0,
                6.0,
                1.0,
                0.0,
                false
        ));

        assertTrue(outcome.applyHeartLoss());
        assertEquals(5.0, outcome.resultingHearts());
    }

    @Test
    void computeVictimOutcomeMobDeathAtThresholdSkipsHeartLoss() {
        // threshold = 5.0, victim has exactly 5.0 → NOT strictly greater → skip loss
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                false,
                true,
                false,
                5.0,
                5.0,
                1.0,
                0.0,
                false
        ));

        assertFalse(outcome.applyHeartLoss());
        assertEquals(5.0, outcome.resultingHearts());
    }

    @Test
    void computeVictimOutcomePartialHeartLossUpdatesCorrectly() {
        // Regular PvP: victim=5, loses=2, min=0 → result=3, delta=-2, no ban
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                false,
                false,
                -1,
                5.0,
                2.0,
                0.0,
                false
        ));

        assertTrue(outcome.applyHeartLoss());
        assertEquals(-2.0, outcome.heartDelta());
        assertEquals(3.0, outcome.resultingHearts());
        assertFalse(outcome.shouldBan());
        assertFalse(outcome.shouldExecuteZeroHeartCommands());
    }

    @Test
    void computeVictimOutcomeMobDeathWithKillerAppliesLossDespiteMobConfig() {
        // Player killer present + mobDeath=true: killer presence takes priority; loss applies
        // even when dontRemoveHeartsFromMobs would normally suppress it.
        DeathOutcome outcome = policy.computeVictimOutcome(new DeathOutcomePolicy.VictimDeathInput(
                false,
                false,
                true,
                true,
                true,
                -1,
                8.0,
                1.0,
                0.0,
                false
        ));

        assertTrue(outcome.applyHeartLoss(),
                "Heart loss must apply when a player killer is present even if dontRemoveHeartsFromMobs is set");
        assertEquals(7.0, outcome.resultingHearts());
    }
}
