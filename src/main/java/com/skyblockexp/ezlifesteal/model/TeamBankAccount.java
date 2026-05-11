package com.skyblockexp.ezlifesteal.model;

import java.util.UUID;

/**
 * Represents a shared heart bank for a team.
 */
public final class TeamBankAccount {

    private final UUID teamId;

    private double hearts;

    public TeamBankAccount(UUID teamId, double hearts) {
        this.teamId = teamId;
        this.hearts = hearts;
    }

    public UUID getTeamId() {
        return teamId;
    }

    public synchronized double getHearts() {
        return hearts;
    }

    public synchronized void setHearts(double hearts) {
        this.hearts = hearts;
    }
}
