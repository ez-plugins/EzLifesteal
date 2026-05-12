package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeamKillBypassServiceTest {

    private DefaultPluginRuntimeServices runtime;
    private TeamKillBypassService service;
    private Logger logger;

    @BeforeEach
    void setUp() {
        runtime = mock(DefaultPluginRuntimeServices.class);
        logger = mock(Logger.class);
        when(runtime.getLogger()).thenReturn(logger);
        when(runtime.getClassLoader()).thenReturn(getClass().getClassLoader());
        service = new TeamKillBypassService(runtime);
    }

    @Test
    void shouldBypass_nullKiller_returnsFalse() {
        Player victim = mock(Player.class);
        assertFalse(service.shouldBypass(null, victim));
    }

    @Test
    void shouldBypass_nullVictim_returnsFalse() {
        Player killer = mock(Player.class);
        assertFalse(service.shouldBypass(killer, null));
    }

    @Test
    void shouldBypass_bothNull_returnsFalse() {
        assertFalse(service.shouldBypass(null, null));
    }

    @Test
    void shouldBypass_bypassDisabled_returnsFalse() {
        when(runtime.isTeamKillBypassEnabled()).thenReturn(false);
        Player killer = mock(Player.class);
        Player victim = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());

        assertFalse(service.shouldBypass(killer, victim));
    }

    @Test
    void shouldBypass_teamsApiNotOnClasspath_returnsFalse() {
        when(runtime.isTeamKillBypassEnabled()).thenReturn(true);
        Player killer = mock(Player.class);
        Player victim = mock(Player.class);
        when(killer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(victim.getUniqueId()).thenReturn(UUID.randomUUID());

        // TeamsAPI class does not exist on test classpath → ClassNotFoundException → false
        assertFalse(service.shouldBypass(killer, victim));
    }
}
