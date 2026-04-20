package com.skyblockexp.ezlifesteal.model;

import java.util.UUID;

public class LifestealProfile {
    private final UUID uniqueId;

    private double hearts;

    private boolean dirty;

    private long revision;

    public LifestealProfile(UUID uniqueId, double hearts) {
        this.uniqueId = uniqueId;
        this.hearts = hearts;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public synchronized double getHearts() {
        return hearts;
    }

    public synchronized void setHearts(double hearts) {
        this.hearts = hearts;
        this.dirty = true;
        this.revision++;
    }

    public synchronized void addHearts(double amount, double max) {
        hearts = Math.min(max, hearts + amount);
        this.dirty = true;
        this.revision++;
    }

    public synchronized void removeHearts(double amount, double min) {
        hearts = Math.max(min, hearts - amount);
        this.dirty = true;
        this.revision++;
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    public synchronized long getRevision() {
        return revision;
    }

    public synchronized void overwriteHeartsFromStorage(double hearts) {
        this.hearts = hearts;
        this.revision++;
        markPersisted();
    }

    public synchronized void markPersisted() {
        this.dirty = false;
    }

    public synchronized void markPersisted(long expectedRevision) {
        if (this.revision == expectedRevision) {
            markPersisted();
        }
    }
}
