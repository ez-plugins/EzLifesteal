package com.skyblockexp.ezlifesteal.service;

public class DeathOutcomePolicy {

    public DeathOutcome computeVictimOutcome(VictimDeathInput input) {
        final boolean applyHeartLoss = shouldApplyHeartLoss(input);
        if (!applyHeartLoss) {
            return new DeathOutcome(false, 0.0, input.victimHeartsBeforeDeath(), false, false);
        }

        // Compute the raw result before applying the min-hearts floor so that the ban/zero-heart
        // conditions can fire even when minHearts > 0 would otherwise clamp the value above 0.
        final double rawResultHearts = input.victimHeartsBeforeDeath() - input.heartsLostOnDeath();
        final double resultingHearts = Math.max(input.minHearts(), rawResultHearts);
        final double heartDelta = resultingHearts - input.victimHeartsBeforeDeath();
        final boolean shouldBan = input.banWhenZeroHearts() && rawResultHearts <= 0.0;
        final boolean shouldExecuteZeroHeartCommands = !shouldBan && rawResultHearts <= 0.0;
        return new DeathOutcome(true, heartDelta, resultingHearts, shouldBan, shouldExecuteZeroHeartCommands);
    }

    public KillerOutcome computeKillerOutcome(KillerInput input) {
        final boolean applyHeartGain = !(input.killerIsAdmin() && input.adminBypassHeartGain());
        if (!applyHeartGain) {
            return new KillerOutcome(false, KillerRewardMode.NONE, 0.0);
        }
        if (input.dropHeartOnDeath()) {
            return new KillerOutcome(true, KillerRewardMode.HEART_ITEM, input.heartsPerKill());
        }
        return new KillerOutcome(true, KillerRewardMode.NUMERIC, input.heartsPerKill());
    }

    private boolean shouldApplyHeartLoss(VictimDeathInput input) {
        if (input.victimIsAdmin() && input.adminBypassHeartLoss()) {
            return false;
        }
        if (!input.mobDeath() || input.hasKiller()) {
            return true;
        }
        if (input.dontRemoveHeartsFromMobs()) {
            return false;
        }
        return input.mobRemoveHeartsGreaterThan() < 0
                || input.victimHeartsBeforeDeath() > input.mobRemoveHeartsGreaterThan();
    }

    public record VictimDeathInput(boolean victimIsAdmin,
                                   boolean adminBypassHeartLoss,
                                   boolean hasKiller,
                                   boolean mobDeath,
                                   boolean dontRemoveHeartsFromMobs,
                                   double mobRemoveHeartsGreaterThan,
                                   double victimHeartsBeforeDeath,
                                   double heartsLostOnDeath,
                                   double minHearts,
                                   boolean banWhenZeroHearts) {
    }

    public record KillerInput(boolean killerIsAdmin,
                              boolean adminBypassHeartGain,
                              boolean dropHeartOnDeath,
                              double heartsPerKill) {
    }
}
