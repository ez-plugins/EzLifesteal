package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamsApiTeamResolverTest {

    private DefaultPluginRuntimeServices runtime;
    private TeamsApiTeamResolver resolver;
    private Logger logger;

    @BeforeEach
    void setUp() {
        runtime = mock(DefaultPluginRuntimeServices.class);
        logger = mock(Logger.class);
        when(runtime.getLogger()).thenReturn(logger);
        // Use the test class's own classloader — TeamsAPI is not on it
        when(runtime.getClassLoader()).thenReturn(getClass().getClassLoader());
        resolver = new TeamsApiTeamResolver(runtime);
    }

    @Test
    void resolveTeam_nullPlayer_returnsEmpty() {
        Optional<TeamsApiTeamResolver.TeamContext> result = resolver.resolveTeam(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTeam_teamsApiNotOnClasspath_returnsEmpty() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        // TeamsAPI class does not exist on the test classpath → ClassNotFoundException → empty
        Optional<TeamsApiTeamResolver.TeamContext> result = resolver.resolveTeam(player);
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTeamByName_null_returnsEmpty() {
        assertTrue(resolver.resolveTeamByName(null).isEmpty());
    }

    @Test
    void resolveTeamByName_blank_returnsEmpty() {
        assertTrue(resolver.resolveTeamByName("   ").isEmpty());
    }

    @Test
    void resolveTeamByName_validUuidString_returnsContextWithParsedUuid() {
        UUID id = UUID.randomUUID();
        // resolveTeamByName will parse the UUID and call resolveTeamName (which will fail via reflection)
        // but it returns the parsed UUID as-is with displayName == nameOrUuid
        Optional<TeamsApiTeamResolver.TeamContext> result = resolver.resolveTeamByName(id.toString());
        assertTrue(result.isPresent());
        assertEquals(id, result.get().teamId());
    }

    @Test
    void resolveTeamByName_invalidName_teamsApiNotAvailable_returnsEmpty() {
        // Non-UUID name; TeamsAPI not on classpath → ClassNotFoundException → empty
        Optional<TeamsApiTeamResolver.TeamContext> result = resolver.resolveTeamByName("SomeTeamName");
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTeamName_teamsApiNotOnClasspath_returnsEmpty() {
        Optional<String> result = resolver.resolveTeamName(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveTeamName_nullUuid_returnsEmpty() {
        Optional<String> result = resolver.resolveTeamName(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void getTeamSize_nullTeamId_returnsZero() {
        assertEquals(0, resolver.getTeamSize(null));
    }

    @Test
    void getTeamSize_teamsApiNotOnClasspath_returnsZero() {
        assertEquals(0, resolver.getTeamSize(UUID.randomUUID()));
    }
}
