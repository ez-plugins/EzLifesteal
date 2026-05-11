package com.skyblockexp.ezlifesteal.runtime.state;

import com.skyblockexp.ezlifesteal.config.BeaconSpawnSettings;
import com.skyblockexp.ezlifesteal.config.ReviveAnimationSettings;
import java.util.List;

public final class ReviveBeaconState {

    private boolean reviveBeaconEnabled;

    private String reviveBeaconVoucherHeartId;

    private boolean reviveBeaconRequireSneak;

    private double reviveBeaconMaxDistance;

    private boolean reviveBeaconConsumeOnFail;

    private boolean reviveBeaconRequireVoucherInBeacon;

    private double reviveBeaconVoucherHoldSeconds;

    private boolean reviveBeaconWhitelistEnabled;

    private List<String> reviveBeaconWhitelistedBeacons;

    private ReviveAnimationSettings reviveAnimationSettings;

    private boolean reviveBeaconBroadcastEnabled;

    private String reviveBeaconBroadcastHoldStartMessageKey;

    private String reviveBeaconBroadcastCompleteMessageKey;

    private BeaconSpawnSettings beaconSpawnSettings;


    ReviveBeaconState() {
        this.reviveBeaconWhitelistedBeacons = List.of();
        this.reviveAnimationSettings = ReviveAnimationSettings.defaults();
        this.reviveBeaconBroadcastHoldStartMessageKey = "beacon-revive-broadcast-hold-start";
        this.reviveBeaconBroadcastCompleteMessageKey = "beacon-revive-broadcast-complete";
        this.beaconSpawnSettings = BeaconSpawnSettings.disabled();
    }

    public static ReviveBeaconState defaults() {
        return new ReviveBeaconState();
    }

    public boolean isReviveBeaconEnabled() {
        return reviveBeaconEnabled;
    }

    public void setReviveBeaconEnabled(boolean reviveBeaconEnabled) {
        this.reviveBeaconEnabled = reviveBeaconEnabled;
    }

    public String getReviveBeaconVoucherHeartId() {
        return reviveBeaconVoucherHeartId;
    }

    public void setReviveBeaconVoucherHeartId(String reviveBeaconVoucherHeartId) {
        this.reviveBeaconVoucherHeartId = reviveBeaconVoucherHeartId;
    }

    public boolean isReviveBeaconRequireSneak() {
        return reviveBeaconRequireSneak;
    }

    public void setReviveBeaconRequireSneak(boolean reviveBeaconRequireSneak) {
        this.reviveBeaconRequireSneak = reviveBeaconRequireSneak;
    }

    public double getReviveBeaconMaxDistance() {
        return reviveBeaconMaxDistance;
    }

    public void setReviveBeaconMaxDistance(double reviveBeaconMaxDistance) {
        this.reviveBeaconMaxDistance = reviveBeaconMaxDistance;
    }

    public boolean isReviveBeaconConsumeOnFail() {
        return reviveBeaconConsumeOnFail;
    }

    public void setReviveBeaconConsumeOnFail(boolean reviveBeaconConsumeOnFail) {
        this.reviveBeaconConsumeOnFail = reviveBeaconConsumeOnFail;
    }

    public boolean isReviveBeaconRequireVoucherInBeacon() {
        return reviveBeaconRequireVoucherInBeacon;
    }

    public void setReviveBeaconRequireVoucherInBeacon(boolean reviveBeaconRequireVoucherInBeacon) {
        this.reviveBeaconRequireVoucherInBeacon = reviveBeaconRequireVoucherInBeacon;
    }

    public double getReviveBeaconVoucherHoldSeconds() {
        return reviveBeaconVoucherHoldSeconds;
    }

    public void setReviveBeaconVoucherHoldSeconds(double reviveBeaconVoucherHoldSeconds) {
        this.reviveBeaconVoucherHoldSeconds = reviveBeaconVoucherHoldSeconds;
    }

    public boolean isReviveBeaconWhitelistEnabled() {
        return reviveBeaconWhitelistEnabled;
    }

    public void setReviveBeaconWhitelistEnabled(boolean reviveBeaconWhitelistEnabled) {
        this.reviveBeaconWhitelistEnabled = reviveBeaconWhitelistEnabled;
    }

    public List<String> getReviveBeaconWhitelistedBeacons() {
        return reviveBeaconWhitelistedBeacons;
    }

    public void setReviveBeaconWhitelistedBeacons(List<String> reviveBeaconWhitelistedBeacons) {
        this.reviveBeaconWhitelistedBeacons = reviveBeaconWhitelistedBeacons;
    }

    public ReviveAnimationSettings getReviveAnimationSettings() {
        return reviveAnimationSettings;
    }

    public void setReviveAnimationSettings(ReviveAnimationSettings reviveAnimationSettings) {
        this.reviveAnimationSettings = reviveAnimationSettings;
    }

    public boolean isReviveBeaconBroadcastEnabled() {
        return reviveBeaconBroadcastEnabled;
    }

    public void setReviveBeaconBroadcastEnabled(boolean reviveBeaconBroadcastEnabled) {
        this.reviveBeaconBroadcastEnabled = reviveBeaconBroadcastEnabled;
    }

    public String getReviveBeaconBroadcastHoldStartMessageKey() {
        return reviveBeaconBroadcastHoldStartMessageKey;
    }

    public void setReviveBeaconBroadcastHoldStartMessageKey(String reviveBeaconBroadcastHoldStartMessageKey) {
        this.reviveBeaconBroadcastHoldStartMessageKey = reviveBeaconBroadcastHoldStartMessageKey;
    }

    public String getReviveBeaconBroadcastCompleteMessageKey() {
        return reviveBeaconBroadcastCompleteMessageKey;
    }

    public void setReviveBeaconBroadcastCompleteMessageKey(String reviveBeaconBroadcastCompleteMessageKey) {
        this.reviveBeaconBroadcastCompleteMessageKey = reviveBeaconBroadcastCompleteMessageKey;
    }

    public BeaconSpawnSettings getBeaconSpawnSettings() {
        return beaconSpawnSettings;
    }

    public void setBeaconSpawnSettings(BeaconSpawnSettings beaconSpawnSettings) {
        this.beaconSpawnSettings = beaconSpawnSettings;
    }
}
