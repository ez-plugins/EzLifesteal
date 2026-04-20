package com.skyblockexp.ezlifesteal.runtime.state;

public final class DropRulesState {

    private boolean dropHeartOnKill;

    private boolean dropHeartOnDeath;

    private boolean dropHeartOnlyWhenKilledByPlayer;

    private String dropHeartId;

    private int dropHeartAmount;

    DropRulesState() {
    }

    public static DropRulesState defaults() {
        return new DropRulesState();
    }

    public boolean isDropHeartOnKill() {
        return dropHeartOnKill;
    }

    public void setDropHeartOnKill(boolean dropHeartOnKill) {
        this.dropHeartOnKill = dropHeartOnKill;
    }

    public boolean isDropHeartOnDeath() {
        return dropHeartOnDeath;
    }

    public void setDropHeartOnDeath(boolean dropHeartOnDeath) {
        this.dropHeartOnDeath = dropHeartOnDeath;
    }

    public boolean isDropHeartOnlyWhenKilledByPlayer() {
        return dropHeartOnlyWhenKilledByPlayer;
    }

    public void setDropHeartOnlyWhenKilledByPlayer(boolean dropHeartOnlyWhenKilledByPlayer) {
        this.dropHeartOnlyWhenKilledByPlayer = dropHeartOnlyWhenKilledByPlayer;
    }

    public String getDropHeartId() {
        return dropHeartId;
    }

    public void setDropHeartId(String dropHeartId) {
        this.dropHeartId = dropHeartId;
    }

    public int getDropHeartAmount() {
        return dropHeartAmount;
    }

    public void setDropHeartAmount(int dropHeartAmount) {
        this.dropHeartAmount = dropHeartAmount;
    }
}
