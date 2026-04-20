package com.skyblockexp.ezlifesteal.runtime.state;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class WorldRulesState {

    private Set<String> enabledWorlds;

    private Set<String> disabledWorlds;

    private Map<String, Double> heartsPerKillOverrides;

    private Map<String, Double> heartsLostOnDeathOverrides;

    private Map<String, Boolean> banWhenZeroOverrides;

    WorldRulesState() {
        this.enabledWorlds = Collections.emptySet();
        this.disabledWorlds = Collections.emptySet();
        this.heartsPerKillOverrides = Map.of();
        this.heartsLostOnDeathOverrides = Map.of();
        this.banWhenZeroOverrides = Map.of();
    }

    public static WorldRulesState defaults() {
        return new WorldRulesState();
    }

    public Set<String> getEnabledWorlds() {
        return enabledWorlds;
    }

    public void setEnabledWorlds(Set<String> enabledWorlds) {
        this.enabledWorlds = enabledWorlds;
    }

    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    public void setDisabledWorlds(Set<String> disabledWorlds) {
        this.disabledWorlds = disabledWorlds;
    }

    public Map<String, Double> getHeartsPerKillOverrides() {
        return heartsPerKillOverrides;
    }

    public void setHeartsPerKillOverrides(Map<String, Double> heartsPerKillOverrides) {
        this.heartsPerKillOverrides = heartsPerKillOverrides;
    }

    public Map<String, Double> getHeartsLostOnDeathOverrides() {
        return heartsLostOnDeathOverrides;
    }

    public void setHeartsLostOnDeathOverrides(Map<String, Double> heartsLostOnDeathOverrides) {
        this.heartsLostOnDeathOverrides = heartsLostOnDeathOverrides;
    }

    public Map<String, Boolean> getBanWhenZeroOverrides() {
        return banWhenZeroOverrides;
    }

    public void setBanWhenZeroOverrides(Map<String, Boolean> banWhenZeroOverrides) {
        this.banWhenZeroOverrides = banWhenZeroOverrides;
    }
}
