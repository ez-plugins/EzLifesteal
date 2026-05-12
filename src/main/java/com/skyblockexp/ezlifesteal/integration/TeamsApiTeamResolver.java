package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

/**
 * Reflection-based accessor for TeamsAPI team data.
 */
public final class TeamsApiTeamResolver {

    public record TeamContext(UUID teamId, String teamName) {
    }

    private final DefaultPluginRuntimeServices runtime;

    public TeamsApiTeamResolver(DefaultPluginRuntimeServices runtime) {
        this.runtime = runtime;
    }

    public Optional<TeamContext> resolveTeam(Player player) {
        if (player == null) {
            return Optional.empty();
        }
        try {
            final Class<?> teamsApiClass = Class.forName("com.skyblockexp.teamsapi.TeamsAPI",
                    false, runtime.getClassLoader());
            final Method isAvailable = teamsApiClass.getMethod("isAvailable");
            final Object availableResult = isAvailable.invoke(null);
            if (!(availableResult instanceof Boolean) || !((Boolean) availableResult)) {
                return Optional.empty();
            }
            final Method getService = teamsApiClass.getMethod("getService");
            final Object teamsService = getService.invoke(null);
            if (teamsService == null) {
                return Optional.empty();
            }
            final Method getPlayerTeam = teamsService.getClass().getMethod("getPlayerTeam", UUID.class);
            final Optional<?> teamOptional = asOptional(getPlayerTeam.invoke(teamsService, player.getUniqueId()));
            if (teamOptional.isEmpty()) {
                return Optional.empty();
            }
            final Object team = teamOptional.get();
            final Method getId = team.getClass().getMethod("getId");
            final Object idResult = getId.invoke(team);
            if (!(idResult instanceof UUID teamId)) {
                return Optional.empty();
            }

            String teamName = teamId.toString();
            try {
                final Method getName = team.getClass().getMethod("getName");
                final Object nameResult = getName.invoke(team);
                if (nameResult instanceof String name && !name.isBlank()) {
                    teamName = name;
                }
            }
            catch (ReflectiveOperationException ignored) {
                // Optional name methods differ by provider/version.
            }
            return Optional.of(new TeamContext(teamId, teamName));
        }
        catch (Throwable throwable) {
            runtime.getLogger().fine("TeamsAPI team resolve unavailable: " + throwable.getMessage());
            return Optional.empty();
        }
    }

    private Optional<?> asOptional(Object value) {
        if (value instanceof Optional<?>) {
            return (Optional<?>) value;
        }
        return Optional.empty();
    }
}
