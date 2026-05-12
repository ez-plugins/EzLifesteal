package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
}
