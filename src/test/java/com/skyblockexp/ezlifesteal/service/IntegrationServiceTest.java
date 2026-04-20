package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.integration.VaultIntegrationState;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IntegrationServiceTest {

    @Test
    void setupVaultSkipsWhenVaultPluginDisabled() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        VaultIntegrationState state = registry.getVaultIntegrationState();
        state.setEconomy(mock(Economy.class));
        Logger logger = mock(Logger.class);
        PluginManager pluginManager = mock(PluginManager.class);

        when(runtime.getLogger()).thenReturn(logger);
        when(pluginManager.isPluginEnabled("Vault")).thenReturn(false);

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            new IntegrationService(runtime, registry).setupVault();

            verify(logger).info(contains("Vault is unavailable"));
        }
    }

    @Test
    void setupVaultHooksProviderWhenAvailable() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        Logger logger = mock(Logger.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        ServicesManager servicesManager = mock(ServicesManager.class);
        @SuppressWarnings("unchecked")
        RegisteredServiceProvider<Economy> registration = mock(RegisteredServiceProvider.class);
        Economy economy = mock(Economy.class);

        when(runtime.getLogger()).thenReturn(logger);
        when(runtime.getServer()).thenReturn(server);
        when(runtime.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(pluginManager.isPluginEnabled("Vault")).thenReturn(true);
        when(servicesManager.getRegistration(Economy.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(economy);
        when(economy.getName()).thenReturn("VaultEco");

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            IntegrationService service = new IntegrationService(runtime, registry);
            service.setupVault();

            verify(logger).info(contains("Hooked into Vault economy provider"));
            org.junit.jupiter.api.Assertions.assertEquals(economy, registry.getVaultIntegrationState().getEconomy());
        }
    }

    @Test
    void setupVaultLogsWarningAndClearsProviderOnUnexpectedFailure() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        Logger logger = mock(Logger.class);
        PluginManager pluginManager = mock(PluginManager.class);
        Server server = mock(Server.class);
        ServicesManager servicesManager = mock(ServicesManager.class);

        registry.getVaultIntegrationState().setEconomy(mock(Economy.class));
        when(runtime.getLogger()).thenReturn(logger);
        when(runtime.getServer()).thenReturn(server);
        when(runtime.getClassLoader()).thenReturn(getClass().getClassLoader());
        when(server.getServicesManager()).thenReturn(servicesManager);
        when(pluginManager.isPluginEnabled("Vault")).thenReturn(true);
        when(servicesManager.getRegistration(Economy.class)).thenThrow(new RuntimeException("boom"));

        try (MockedStatic<Bukkit> bukkit = org.mockito.Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);

            new IntegrationService(runtime, registry).setupVault();

            verify(logger).warning(contains("Failed to hook into Vault economy"));
            org.junit.jupiter.api.Assertions.assertNull(registry.getVaultIntegrationState().getEconomy());
        }
    }

    @Test
    void ensureVaultEconomyClassesAvailableDisablesFlagWhenClassLoadFails() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        ClassLoader failingLoader = new ClassLoader(null) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if ("net.milkbowl.vault.economy.Economy".equals(name)) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name);
            }
        };
        when(runtime.getClassLoader()).thenReturn(failingLoader);

        IntegrationService service = new IntegrationService(runtime, registry);

        assertFalse(service.ensureVaultEconomyClassesAvailable());
        assertFalse(registry.getVaultIntegrationState().isEconomyClassesAvailable());
    }

    @Test
    void ensureVaultEconomyClassesAvailableReturnsFalseWhenAlreadyMarkedUnavailable() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        registry.getVaultIntegrationState().setEconomyClassesAvailable(false);

        IntegrationService service = new IntegrationService(runtime, registry);

        assertFalse(service.ensureVaultEconomyClassesAvailable());
        verify(runtime, never()).getClassLoader();
    }

    @Test
    void ensureVaultEconomyClassesAvailableReturnsTrueWhenClassExists() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        Registry registry = new Registry();
        when(runtime.getClassLoader()).thenReturn(getClass().getClassLoader());

        assertTrue(new IntegrationService(runtime, registry).ensureVaultEconomyClassesAvailable());
    }
}
