package com.skyblockexp.ezlifesteal.runtime.state;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HeartRulesState {

    private double heartsPerKill;

    private double heartsLostOnDeath;

    private boolean banWhenZeroHearts;

    private boolean adminBypassHeartLoss;

    private boolean adminBypassHeartGain;

    private boolean globalLifestealEnabled;

    private boolean teamKillBypassWithTeamsApi;

    private List<String> teamKillBypassExemptWorlds = Collections.emptyList();

    private int teamKillBypassMinTeamSize = 1;

    private boolean teamBankEnabled;

    private double teamBankMaxHearts;

    private Map<String, Double> teamBankPerTeamMaxHearts = Map.of();

    HeartRulesState() {
    }

    public static HeartRulesState defaults() {
        return new HeartRulesState();
    }

    public double getHeartsPerKill() {
        return heartsPerKill;
    }

    public void setHeartsPerKill(double heartsPerKill) {
        this.heartsPerKill = heartsPerKill;
    }

    public double getHeartsLostOnDeath() {
        return heartsLostOnDeath;
    }

    public void setHeartsLostOnDeath(double heartsLostOnDeath) {
        this.heartsLostOnDeath = heartsLostOnDeath;
    }

    public boolean isBanWhenZeroHearts() {
        return banWhenZeroHearts;
    }

    public void setBanWhenZeroHearts(boolean banWhenZeroHearts) {
        this.banWhenZeroHearts = banWhenZeroHearts;
    }

    public boolean isAdminBypassHeartLoss() {
        return adminBypassHeartLoss;
    }

    public void setAdminBypassHeartLoss(boolean adminBypassHeartLoss) {
        this.adminBypassHeartLoss = adminBypassHeartLoss;
    }

    public boolean isAdminBypassHeartGain() {
        return adminBypassHeartGain;
    }

    public void setAdminBypassHeartGain(boolean adminBypassHeartGain) {
        this.adminBypassHeartGain = adminBypassHeartGain;
    }

    public boolean isGlobalLifestealEnabled() {
        return globalLifestealEnabled;
    }

    public void setGlobalLifestealEnabled(boolean globalLifestealEnabled) {
        this.globalLifestealEnabled = globalLifestealEnabled;
    }

    public boolean isTeamKillBypassWithTeamsApi() {
        return teamKillBypassWithTeamsApi;
    }

    public void setTeamKillBypassWithTeamsApi(boolean teamKillBypassWithTeamsApi) {
        this.teamKillBypassWithTeamsApi = teamKillBypassWithTeamsApi;
    }

    public List<String> getTeamKillBypassExemptWorlds() {
        return teamKillBypassExemptWorlds;
    }

    public void setTeamKillBypassExemptWorlds(List<String> teamKillBypassExemptWorlds) {
        this.teamKillBypassExemptWorlds = teamKillBypassExemptWorlds == null ? Collections.emptyList() : teamKillBypassExemptWorlds;
    }

    public int getTeamKillBypassMinTeamSize() {
        return teamKillBypassMinTeamSize;
    }

    public void setTeamKillBypassMinTeamSize(int teamKillBypassMinTeamSize) {
        this.teamKillBypassMinTeamSize = Math.max(1, teamKillBypassMinTeamSize);
    }

    public boolean isTeamBankEnabled() {
        return teamBankEnabled;
    }

    public void setTeamBankEnabled(boolean teamBankEnabled) {
        this.teamBankEnabled = teamBankEnabled;
    }

    public double getTeamBankMaxHearts() {
        return teamBankMaxHearts;
    }

    public void setTeamBankMaxHearts(double teamBankMaxHearts) {
        this.teamBankMaxHearts = teamBankMaxHearts;
    }

    public Map<String, Double> getTeamBankPerTeamMaxHearts() {
        return teamBankPerTeamMaxHearts;
    }

    public void setTeamBankPerTeamMaxHearts(Map<String, Double> teamBankPerTeamMaxHearts) {
        this.teamBankPerTeamMaxHearts = teamBankPerTeamMaxHearts == null ? Map.of() : teamBankPerTeamMaxHearts;
    }
}
