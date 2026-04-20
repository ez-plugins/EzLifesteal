package com.skyblockexp.ezlifesteal.service;

public record KillerOutcome(boolean applyHeartGain,
                            KillerRewardMode rewardMode,
                            double numericHeartGain) {
}
