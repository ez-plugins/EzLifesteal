package com.skyblockexp.ezlifesteal.service;

public record DeathOutcome(boolean applyHeartLoss,
                           double heartDelta,
                           double resultingHearts,
                           boolean shouldBan,
                           boolean shouldExecuteZeroHeartCommands) {
}
