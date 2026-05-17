package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import com.skyblockexp.ezlifesteal.integration.TeamsApiTeamResolver;
import com.skyblockexp.ezlifesteal.runtime.state.GameplayState;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesSmallTests {

    @Test
    void ensureAdditionalConfigFiles_delegatesToConfigLoader() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        final Registry registry = new Registry();
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        // inject mock ConfigLoader into private field
        final ConfigLoader loader = mock(ConfigLoader.class);
        final Field loaderField = DefaultPluginRuntimeServices.class.getDeclaredField("configLoader");
        loaderField.setAccessible(true);
        loaderField.set(services, loader);

        services.ensureAdditionalConfigFiles();

        verify(loader, atLeastOnce()).ensureResources(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void ensureVaultEconomyClassesAvailable_returnsFalse_whenClassMissing_and_updatesRegistry() {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        final Registry registry = new Registry();
        // ensure the registry initially thinks classes are available
        registry.getVaultIntegrationState().setEconomyClassesAvailable(true);

        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        final boolean available = services.ensureVaultEconomyClassesAvailable();

        // The runtime classpath may or may not include Vault; ensure the registry reflects the method result
        assertEquals(available, registry.getVaultIntegrationState().isEconomyClassesAvailable());
    }

    @Test
    void teamKillBypassAndBankDelegatesToHeartRulesState() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final GameplayState gameplayState = getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setTeamKillBypassWithTeamsApi(true);
        gameplayState.getHeartRulesState().setTeamKillBypassExemptWorlds(List.of("hub"));
        gameplayState.getHeartRulesState().setTeamKillBypassMinTeamSize(2);
        gameplayState.getHeartRulesState().setTeamBankEnabled(true);
        gameplayState.getHeartRulesState().setTeamBankMaxHearts(300.0);

        assertTrue(services.isTeamKillBypassEnabled());
        assertEquals(List.of("hub"), services.getTeamKillBypassExemptWorlds());
        assertEquals(2, services.getTeamKillBypassMinTeamSize());
        assertTrue(services.isTeamBankEnabled());
        assertEquals(300.0, services.getTeamBankMaxHearts());
    }

    @Test
    void getTeamBankMaxHeartsForTeam_nullId_returnsGlobal() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final GameplayState gameplayState = getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setTeamBankMaxHearts(150.0);

        assertEquals(150.0, services.getTeamBankMaxHeartsForTeam(null));
    }

    @Test
    void getTeamBankMaxHeartsForTeam_withUuidOverride_returnsOverride() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final GameplayState gameplayState = getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setTeamBankMaxHearts(100.0);

        final UUID teamId = UUID.randomUUID();
        gameplayState.getHeartRulesState().setTeamBankPerTeamMaxHearts(Map.of(teamId.toString(), 250.0));

        assertEquals(250.0, services.getTeamBankMaxHeartsForTeam(teamId));
    }

    @Test
    void getTeamBankMaxHeartsForTeam_withNameOverrideViaResolver_returnsOverride() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final GameplayState gameplayState = getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setTeamBankMaxHearts(100.0);
        gameplayState.getHeartRulesState().setTeamBankPerTeamMaxHearts(Map.of("TeamAlpha", 400.0));

        final UUID teamId = UUID.randomUUID();
        // Inject a mock resolver that returns "TeamAlpha" for the given UUID
        final TeamsApiTeamResolver resolver = mock(TeamsApiTeamResolver.class);
        when(resolver.resolveTeamName(teamId)).thenReturn(Optional.of("TeamAlpha"));
        setField(services, "teamsApiTeamResolver", resolver);

        assertEquals(400.0, services.getTeamBankMaxHeartsForTeam(teamId));
    }

    @Test
    void getTeamBankMaxHeartsForTeam_noOverride_returnsGlobal() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final GameplayState gameplayState = getField(services, "gameplayState");
        gameplayState.getHeartRulesState().setTeamBankMaxHearts(200.0);

        assertEquals(200.0, services.getTeamBankMaxHeartsForTeam(UUID.randomUUID()));
    }

    @Test
    void getTeamBankAdminService_returnsInjectedService() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final com.skyblockexp.ezlifesteal.service.TeamBankAdminService mockAdminService =
                mock(com.skyblockexp.ezlifesteal.service.TeamBankAdminService.class);
        setField(services, "teamBankAdminService", mockAdminService);

        assertSame(mockAdminService, services.getTeamBankAdminService());
    }

    @Test
    void getTeamsApiTeamResolver_returnsInjectedResolver() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());

        final TeamsApiTeamResolver mockResolver = mock(TeamsApiTeamResolver.class);
        setField(services, "teamsApiTeamResolver", mockResolver);

        assertSame(mockResolver, services.getTeamsApiTeamResolver());
    }

    @Test
    void shouldBypassForTeamKill_nullService_returnsFalse() throws Exception {
        final EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));
        final DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, new Registry());
        // teamKillBypassService is not initialized without initializeCoreState(); it remains null

        assertFalse(services.shouldBypassForTeamKill(mock(org.bukkit.entity.Player.class),
                mock(org.bukkit.entity.Player.class)));
    }

    @SuppressWarnings("unchecked")
    private static <T> T getField(Object target, String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
