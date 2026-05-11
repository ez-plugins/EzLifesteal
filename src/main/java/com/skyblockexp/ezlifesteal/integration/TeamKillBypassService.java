package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Optional TeamsAPI bridge that determines whether a killer/victim pair are in the same team.
 */
public final class TeamKillBypassService {

    private final DefaultPluginRuntimeServices runtime;

    public TeamKillBypassService(DefaultPluginRuntimeServices runtime) {
        this.runtime = runtime;
    }

    public boolean shouldBypass(Player killer, Player victim) {
        if (killer == null || victim == null) {
            return false;
        }
        if (!runtime.isTeamKillBypassEnabled()) {
            return false;
        }
        try {
            final Class<?> teamsApiClass = Class.forName("com.skyblockexp.teamsapi.TeamsAPI",
                    false, runtime.getClassLoader());
            final Method isAvailable = teamsApiClass.getMethod("isAvailable");
            final Object availableResult = isAvailable.invoke(null);
            if (!(availableResult instanceof Boolean) || !((Boolean) availableResult)) {
                return false;
            }

            final Method getService = teamsApiClass.getMethod("getService");
            final Object teamsService = getService.invoke(null);
            if (teamsService == null) {
                return false;
            }

            final Method getPlayerTeam = teamsService.getClass().getMethod("getPlayerTeam", UUID.class);
            final Optional<?> killerTeam = asOptional(getPlayerTeam.invoke(teamsService, killer.getUniqueId()));
            final Optional<?> victimTeam = asOptional(getPlayerTeam.invoke(teamsService, victim.getUniqueId()));
            if (killerTeam.isEmpty() || victimTeam.isEmpty()) {
                return false;
            }

            final Object killerTeamValue = killerTeam.get();
            final Object victimTeamValue = victimTeam.get();
            if (killerTeamValue.equals(victimTeamValue)) {
                return true;
            }

            final Method getId = killerTeamValue.getClass().getMethod("getId");
            final Object killerId = getId.invoke(killerTeamValue);
            final Object victimId = getId.invoke(victimTeamValue);
            return killerId != null && killerId.equals(victimId);
        }
        catch (Throwable throwable) {
            runtime.getLogger().fine("TeamsAPI bypass check unavailable: " + throwable.getMessage());
            return false;
        }
    }

    private Optional<?> asOptional(Object value) {
        if (value instanceof Optional<?>) {
            return (Optional<?>) value;
        }
        return Optional.empty();
    }
}
