package com.skyblockexp.ezlifesteal.runtime.state;

import com.skyblockexp.ezlifesteal.model.MobReward;
import java.util.Map;
import org.bukkit.entity.EntityType;

public final class MobRulesState {

    private Map<EntityType, MobReward> mobRewards;

    private boolean dontRemoveHeartsFromMobs;

    private double mobRemoveHeartsGreaterThan;


    MobRulesState() {
        this.mobRewards = Map.of();
    }

    public static MobRulesState defaults() {
        return new MobRulesState();
    }

    public Map<EntityType, MobReward> getMobRewards() {
        return mobRewards;
    }

    public void setMobRewards(Map<EntityType, MobReward> mobRewards) {
        this.mobRewards = mobRewards;
    }

    public boolean isDontRemoveHeartsFromMobs() {
        return dontRemoveHeartsFromMobs;
    }

    public void setDontRemoveHeartsFromMobs(boolean dontRemoveHeartsFromMobs) {
        this.dontRemoveHeartsFromMobs = dontRemoveHeartsFromMobs;
    }

    public double getMobRemoveHeartsGreaterThan() {
        return mobRemoveHeartsGreaterThan;
    }

    public void setMobRemoveHeartsGreaterThan(double mobRemoveHeartsGreaterThan) {
        this.mobRemoveHeartsGreaterThan = mobRemoveHeartsGreaterThan;
    }
}
