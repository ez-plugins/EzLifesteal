package com.skyblockexp.ezlifesteal.runtime.state;

import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;

public final class GameplayState {

    private final HeartRulesState heartRulesState;

    private final DropRulesState dropRulesState;

    private final ReviveBeaconState reviveBeaconState;

    private final WorldRulesState worldRulesState;

    private final MobRulesState mobRulesState;

    private KillStreakSettings killStreakSettings;

    GameplayState(
            HeartRulesState heartRulesState,
            DropRulesState dropRulesState,
            ReviveBeaconState reviveBeaconState,
            WorldRulesState worldRulesState,
            MobRulesState mobRulesState,
            KillStreakSettings killStreakSettings
    ) {
        this.heartRulesState = heartRulesState;
        this.dropRulesState = dropRulesState;
        this.reviveBeaconState = reviveBeaconState;
        this.worldRulesState = worldRulesState;
        this.mobRulesState = mobRulesState;
        this.killStreakSettings = killStreakSettings;
    }

    public static GameplayState defaults() {
        return new GameplayState(
                HeartRulesState.defaults(),
                DropRulesState.defaults(),
                ReviveBeaconState.defaults(),
                WorldRulesState.defaults(),
                MobRulesState.defaults(),
                KillStreakSettings.disabled()
        );
    }

    public HeartRulesState getHeartRulesState() {
        return heartRulesState;
    }

    public DropRulesState getDropRulesState() {
        return dropRulesState;
    }

    public ReviveBeaconState getReviveBeaconState() {
        return reviveBeaconState;
    }

    public WorldRulesState getWorldRulesState() {
        return worldRulesState;
    }

    public MobRulesState getMobRulesState() {
        return mobRulesState;
    }

    public KillStreakSettings getKillStreakSettings() {
        return killStreakSettings;
    }

    public void setKillStreakSettings(KillStreakSettings killStreakSettings) {
        this.killStreakSettings = killStreakSettings;
    }
}
