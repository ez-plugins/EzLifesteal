package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.killstreak.KillStreakSettings;
import com.skyblockexp.ezlifesteal.runtime.state.DropRulesState;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import com.skyblockexp.ezlifesteal.runtime.state.HeartRulesState;
import com.skyblockexp.ezlifesteal.runtime.state.MobRulesState;
import com.skyblockexp.ezlifesteal.runtime.state.ReviveBeaconState;
import com.skyblockexp.ezlifesteal.runtime.state.WorldRulesState;
import java.util.Set;

/**
 * Applies parsed gameplay settings into registry gameplay state.
 */
public class GameplayStateApplier {

    public void apply(
            GameplayState gameplayState,
            LifestealSettingsFactory.LifestealSettings lifestealSettings,
            AdminSettingsFactory.AdminSettings adminSettings,
            KillStreakSettings killStreakSettings,
            Set<String> enabledWorlds,
            Set<String> disabledWorlds
    ) {
        final HeartRulesState heartRulesState = gameplayState.getHeartRulesState();
        final DropRulesState dropRulesState = gameplayState.getDropRulesState();
        final ReviveBeaconState reviveBeaconState = gameplayState.getReviveBeaconState();
        final WorldRulesState worldRulesState = gameplayState.getWorldRulesState();
        final MobRulesState mobRulesState = gameplayState.getMobRulesState();

        heartRulesState.setHeartsPerKill(lifestealSettings.heartsPerKill());
        heartRulesState.setHeartsLostOnDeath(lifestealSettings.heartsLostOnDeath());
        heartRulesState.setTeamKillBypassWithTeamsApi(lifestealSettings.teamKillBypassWithTeamsApi());
        heartRulesState.setTeamKillBypassExemptWorlds(lifestealSettings.teamKillBypassExemptWorlds());
        heartRulesState.setTeamKillBypassMinTeamSize(lifestealSettings.teamKillBypassMinTeamSize());
        heartRulesState.setTeamBankEnabled(lifestealSettings.teamBankEnabled());
        heartRulesState.setTeamBankMaxHearts(lifestealSettings.teamBankMaxHearts());
        heartRulesState.setTeamBankPerTeamMaxHearts(lifestealSettings.teamBankPerTeamMaxHearts());
        heartRulesState.setBanWhenZeroHearts(lifestealSettings.banWhenZeroHearts());
        heartRulesState.setGlobalLifestealEnabled(lifestealSettings.globalLifestealEnabled());
        heartRulesState.setAdminBypassHeartLoss(adminSettings.adminBypassHeartLoss());
        heartRulesState.setAdminBypassHeartGain(adminSettings.adminBypassHeartGain());

        dropRulesState.setDropHeartOnKill(lifestealSettings.dropHeartOnKill());
        dropRulesState.setDropHeartOnDeath(lifestealSettings.dropHeartOnDeath());
        dropRulesState.setDropHeartOnlyWhenKilledByPlayer(lifestealSettings.dropHeartOnlyWhenKilledByPlayer());
        dropRulesState.setDropHeartId(lifestealSettings.dropHeartId());
        dropRulesState.setDropHeartAmount(lifestealSettings.dropHeartAmount());

        reviveBeaconState.setReviveBeaconEnabled(lifestealSettings.reviveBeaconEnabled());
        reviveBeaconState.setReviveBeaconVoucherHeartId(lifestealSettings.reviveBeaconVoucherHeartId());
        reviveBeaconState.setReviveBeaconRequireSneak(lifestealSettings.reviveBeaconRequireSneak());
        reviveBeaconState.setReviveBeaconMaxDistance(lifestealSettings.reviveBeaconMaxDistance());
        reviveBeaconState.setReviveBeaconConsumeOnFail(lifestealSettings.reviveBeaconConsumeOnFail());
        reviveBeaconState.setReviveBeaconRequireVoucherInBeacon(lifestealSettings.reviveBeaconRequireVoucherInBeacon());
        reviveBeaconState.setReviveBeaconVoucherHoldSeconds(lifestealSettings.reviveBeaconVoucherHoldSeconds());
        reviveBeaconState.setReviveBeaconWhitelistEnabled(lifestealSettings.reviveBeaconWhitelistEnabled());
        reviveBeaconState.setReviveBeaconWhitelistedBeacons(lifestealSettings.reviveBeaconWhitelistedBeacons());
        reviveBeaconState.setReviveAnimationSettings(lifestealSettings.reviveAnimationSettings());
        reviveBeaconState.setReviveBeaconBroadcastEnabled(lifestealSettings.reviveBeaconBroadcastEnabled());
        reviveBeaconState.setReviveBeaconBroadcastHoldStartMessageKey(
                lifestealSettings.reviveBeaconBroadcastHoldStartMessageKey()
        );
        reviveBeaconState.setReviveBeaconBroadcastCompleteMessageKey(
                lifestealSettings.reviveBeaconBroadcastCompleteMessageKey()
        );
        reviveBeaconState.setBeaconSpawnSettings(lifestealSettings.beaconSpawnSettings());

        mobRulesState.setDontRemoveHeartsFromMobs(lifestealSettings.dontRemoveHeartsFromMobs());
        mobRulesState.setMobRemoveHeartsGreaterThan(lifestealSettings.mobRemoveHeartsGreaterThan());

        worldRulesState.setEnabledWorlds(enabledWorlds);
        worldRulesState.setDisabledWorlds(disabledWorlds);

        gameplayState.setKillStreakSettings(killStreakSettings);
    }
}
