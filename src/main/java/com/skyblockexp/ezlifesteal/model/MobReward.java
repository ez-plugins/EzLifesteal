package com.skyblockexp.ezlifesteal.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.bukkit.entity.EntityType;

public class MobReward {

    private final EntityType entityType;

    private final double hearts;

    private final Set<String> allowedWorlds;

    private final Set<String> blockedWorlds;

    private final String permission;


    public MobReward(EntityType entityType,
                     double hearts,
                     Set<String> allowedWorlds,
                     Set<String> blockedWorlds,
                     String permission) {
        this.entityType = Objects.requireNonNull(entityType, "entityType");
        this.hearts = hearts;
        this.allowedWorlds = normalizeWorldSet(allowedWorlds);
        this.blockedWorlds = normalizeWorldSet(blockedWorlds);
        this.permission = permission;
    }

    private Set<String> normalizeWorldSet(Set<String> worlds) {
        if (worlds == null || worlds.isEmpty()) {
            return Collections.emptySet();
        }

        final Set<String> normalized = new LinkedHashSet<>();
        for (String world : worlds) {
            if (world != null && !world.isBlank()) {
                normalized.add(world.toLowerCase(Locale.ROOT));
            }
        }
        return normalized.isEmpty() ? Collections.emptySet() : Set.copyOf(normalized);
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public double getHearts() {
        return hearts;
    }

    public String getPermission() {
        return permission;
    }

    public boolean isWorldAllowed(String worldName) {
        if (worldName == null || worldName.isBlank()) {
            return allowedWorlds.isEmpty();
        }
        final String normalized = worldName.toLowerCase(Locale.ROOT);
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(normalized)) {
            return false;
        }
        return !blockedWorlds.contains(normalized);
    }
}
