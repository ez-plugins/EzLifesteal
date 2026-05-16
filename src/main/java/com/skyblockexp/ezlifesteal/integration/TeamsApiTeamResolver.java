package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.lang.reflect.Method;
import java.util.Collection;
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

    /**
     * Returns the display name for a team UUID, if resolvable via TeamsAPI.
     */
    public Optional<String> resolveTeamName(UUID teamId) {
        try {
            final Object teamsService = getTeamsService();
            if (teamsService == null) {
                return Optional.empty();
            }
            final Method getTeam = teamsService.getClass().getMethod("getTeam", UUID.class);
            final Object teamOptResult = getTeam.invoke(teamsService, teamId);
            final Optional<?> teamOptional = asOptional(teamOptResult);
            if (teamOptional.isEmpty()) {
                return Optional.empty();
            }
            final Object team = teamOptional.get();
            final Method getName = team.getClass().getMethod("getName");
            final Object nameResult = getName.invoke(team);
            if (nameResult instanceof String name && !name.isBlank()) {
                return Optional.of(name);
            }
            return Optional.empty();
        }
        catch (Throwable throwable) {
            runtime.getLogger().fine("TeamsAPI resolveTeamName unavailable: " + throwable.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Resolves a team by name or UUID string. Accepts a raw UUID string as primary format;
     * falls back to name-based lookup via TeamsAPI reflection.
     */
    public Optional<TeamContext> resolveTeamByName(String nameOrUuid) {
        if (nameOrUuid == null || nameOrUuid.isBlank()) {
            return Optional.empty();
        }
        // Try UUID parse first
        try {
            final UUID parsed = UUID.fromString(nameOrUuid);
            final Optional<String> resolvedName = resolveTeamName(parsed);
            final String displayName = resolvedName.orElse(nameOrUuid);
            return Optional.of(new TeamContext(parsed, displayName));
        }
        catch (IllegalArgumentException ignored) {
            // Not a UUID; fall through to name-based lookup
        }
        // Name-based lookup
        try {
            final Object teamsService = getTeamsService();
            if (teamsService == null) {
                return Optional.empty();
            }
            // Try direct getTeamByName first
            try {
                final Method getByName = teamsService.getClass().getMethod("getTeamByName", String.class);
                final Object result = getByName.invoke(teamsService, nameOrUuid);
                final Optional<?> optional = asOptional(result);
                if (optional.isPresent()) {
                    return extractContext(optional.get());
                }
            }
            catch (NoSuchMethodException ignored) {
                // Method not available; fall through to getAllTeams search
            }
            // getAllTeams filter
            final Method getAllTeams = teamsService.getClass().getMethod("getAllTeams");
            final Object allTeamsResult = getAllTeams.invoke(teamsService);
            if (allTeamsResult instanceof Collection<?> teams) {
                for (Object team : teams) {
                    try {
                        final Method getName = team.getClass().getMethod("getName");
                        final Object nameResult = getName.invoke(team);
                        if (nameOrUuid.equalsIgnoreCase(nameResult instanceof String s ? s : null)) {
                            return extractContext(team);
                        }
                    }
                    catch (ReflectiveOperationException ignored) {
                    }
                }
            }
            return Optional.empty();
        }
        catch (Throwable throwable) {
            runtime.getLogger().fine("TeamsAPI resolveTeamByName unavailable: " + throwable.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Returns the member count for the given team UUID. Returns 0 on any failure.
     */
    public int getTeamSize(UUID teamId) {
        if (teamId == null) {
            return 0;
        }
        try {
            final Object teamsService = getTeamsService();
            if (teamsService == null) {
                return 0;
            }
            final Method getTeam = teamsService.getClass().getMethod("getTeam", UUID.class);
            final Object teamOptResult = getTeam.invoke(teamsService, teamId);
            final Optional<?> teamOptional = asOptional(teamOptResult);
            if (teamOptional.isEmpty()) {
                return 0;
            }
            final Object team = teamOptional.get();
            // Try getMembers().size() first
            try {
                final Method getMembers = team.getClass().getMethod("getMembers");
                final Object members = getMembers.invoke(team);
                if (members instanceof Collection<?> col) {
                    return col.size();
                }
            }
            catch (NoSuchMethodException ignored) {
            }
            // Try getMemberCount() as int
            try {
                final Method getMemberCount = team.getClass().getMethod("getMemberCount");
                final Object count = getMemberCount.invoke(team);
                if (count instanceof Integer i) {
                    return i;
                }
            }
            catch (NoSuchMethodException ignored) {
            }
            return 0;
        }
        catch (Throwable throwable) {
            runtime.getLogger().fine("TeamsAPI getTeamSize unavailable: " + throwable.getMessage());
            return 0;
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Object getTeamsService() throws ReflectiveOperationException {
        final Class<?> teamsApiClass = Class.forName("com.skyblockexp.teamsapi.TeamsAPI",
                false, runtime.getClassLoader());
        final Method isAvailable = teamsApiClass.getMethod("isAvailable");
        final Object availableResult = isAvailable.invoke(null);
        if (!(availableResult instanceof Boolean) || !((Boolean) availableResult)) {
            return null;
        }
        final Method getService = teamsApiClass.getMethod("getService");
        return getService.invoke(null);
    }

    private Optional<TeamContext> extractContext(Object team) {
        try {
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
            }
            return Optional.of(new TeamContext(teamId, teamName));
        }
        catch (ReflectiveOperationException exception) {
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
