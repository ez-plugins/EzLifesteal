package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultPluginRuntimeServicesVaultTest {

    @Test
    void setupVaultLeavesEconomyNullWhenVaultPluginDisabled() {
        RuntimeHarness harness = runtimeHarness();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.isPluginEnabled("Vault")).thenReturn(false);

            harness.services.setupVault();

            assertNull(harness.registry.getVaultIntegrationState().getEconomy());
            verify(harness.logger, never()).info(contains("Hooked into Vault economy provider"));
        }
    }

    @Test
    void setupVaultSkipsIntegrationWhenVaultEconomyClassesUnavailable() {
        RuntimeHarness harness = runtimeHarness();
        DefaultPluginRuntimeServices spy = spy(harness.services);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.isPluginEnabled("Vault")).thenReturn(true);
            doReturn(false).when(spy).ensureVaultEconomyClassesAvailable();

            spy.setupVault();

            assertNull(harness.registry.getVaultIntegrationState().getEconomy());
            verify(harness.server, never()).getServicesManager();
        }
    }

    @Test
    void setupVaultLeavesEconomyNullWhenRegistrationMissingOrProviderNull() {
        RuntimeHarness missingRegistrationHarness = runtimeHarness();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(missingRegistrationHarness.pluginManager);
            when(missingRegistrationHarness.pluginManager.isPluginEnabled("Vault")).thenReturn(true);
            when(missingRegistrationHarness.servicesManager.getRegistration(Economy.class)).thenReturn(null);

            missingRegistrationHarness.services.setupVault();

            assertNull(missingRegistrationHarness.registry.getVaultIntegrationState().getEconomy());
        }

        RuntimeHarness nullProviderHarness = runtimeHarness();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<Economy> registration = mock(RegisteredServiceProvider.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(nullProviderHarness.pluginManager);
            when(nullProviderHarness.pluginManager.isPluginEnabled("Vault")).thenReturn(true);
            when(nullProviderHarness.servicesManager.getRegistration(Economy.class)).thenReturn(registration);
            when(registration.getProvider()).thenReturn(null);

            nullProviderHarness.services.setupVault();

            assertNull(nullProviderHarness.registry.getVaultIntegrationState().getEconomy());
        }
    }

    @Test
    void setupVaultStoresProviderAndLogsHookMessageWhenProviderPresent() {
        RuntimeHarness harness = runtimeHarness();
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<Economy> registration = mock(RegisteredServiceProvider.class);
        Economy economy = mock(Economy.class);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.isPluginEnabled("Vault")).thenReturn(true);
            when(harness.servicesManager.getRegistration(Economy.class)).thenReturn(registration);
            when(registration.getProvider()).thenReturn(economy);
            when(economy.getName()).thenReturn("TestEco");

            harness.services.setupVault();

            assertSame(economy, harness.registry.getVaultIntegrationState().getEconomy());
            verify(harness.logger).info("Hooked into Vault economy provider: TestEco.");
        }
    }

    @Test
    void setupVaultResetsStateAndLogsWarningWhenThrowableOccursDuringSetup() {
        RuntimeHarness harness = runtimeHarness();

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class, CALLS_REAL_METHODS)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(harness.pluginManager);
            when(harness.pluginManager.isPluginEnabled("Vault")).thenReturn(true);
            when(harness.servicesManager.getRegistration(Economy.class)).thenThrow(new RuntimeException("boom"));

            harness.services.setupVault();

            assertNull(harness.registry.getVaultIntegrationState().getEconomy());
            verify(harness.logger).warning("Failed to hook into Vault economy: boom");
        }
    }

    private static RuntimeHarness runtimeHarness() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Logger logger = mock(Logger.class);
        Registry registry = new Registry();
        Server server = mock(Server.class);
        PluginManager pluginManager = mock(PluginManager.class);
        ServicesManager servicesManager = mock(ServicesManager.class);

        when(plugin.getLogger()).thenReturn(logger);
        when(plugin.getServer()).thenReturn(server);
        when(server.getServicesManager()).thenReturn(servicesManager);

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);
        return new RuntimeHarness(services, registry, logger, server, pluginManager, servicesManager);
    }

    private record RuntimeHarness(
            DefaultPluginRuntimeServices services,
            Registry registry,
            Logger logger,
            Server server,
            PluginManager pluginManager,
            ServicesManager servicesManager
    ) {
    }
}
