package com.skyblockexp.ezlifesteal.service;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;


public class IntegrationService {

    private final Registry registry;

    private final DefaultPluginRuntimeServices services;


    public IntegrationService(DefaultPluginRuntimeServices services, Registry registry) {
        this.services = services;
        this.registry = registry;
    }

    public void setupVault() {
        final Economy previous = registry.getVaultIntegrationState().getEconomy();
        registry.getVaultIntegrationState().setEconomy(null);
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            if (previous != null) {
                services.getLogger().info("Vault is unavailable; currency rewards are disabled.");
            }
            return;
        }
        if (!ensureVaultEconomyClassesAvailable()) {
            if (previous != null) {
                services.getLogger().info("Vault economy classes are unavailable; currency rewards are disabled.");
            }
            else {
                services.getLogger().fine("Vault economy classes are unavailable; skipping currency integration.");
            }
            return;
        }
        try {
            final RegisteredServiceProvider<Economy> registration =
                    services.getServer().getServicesManager().getRegistration(Economy.class);
            if (registration == null) {
                if (previous != null) {
                    services.getLogger().info("No Vault economy provider detected; currency rewards are disabled.");
                }
                return;
            }
            final Economy provider = registration.getProvider();
            if (provider == null) {
                if (previous != null) {
                    services.getLogger().info("Vault returned a null economy provider; currency rewards are disabled.");
                }
                return;
            }
            registry.getVaultIntegrationState().setEconomy(provider);
            if (previous == null || !previous.getName().equalsIgnoreCase(provider.getName())) {
                services.getLogger().info("Hooked into Vault economy provider: " + provider.getName() + '.');
            }
        }
        catch (NoClassDefFoundError error) {
            registry.getVaultIntegrationState().setEconomy(null);
            registry.getVaultIntegrationState().setEconomyClassesAvailable(false);
            if (previous != null) {
                services.getLogger().info("Vault economy classes are unavailable; currency rewards are disabled.");
            }
            else {
                services.getLogger().fine("Vault economy classes are unavailable; skipping currency integration.");
            }
        }
        catch (Throwable throwable) {
            registry.getVaultIntegrationState().setEconomy(null);
            services.getLogger().warning("Failed to hook into Vault economy: " + throwable.getMessage());
        }
    }

    public boolean ensureVaultEconomyClassesAvailable() {
        if (!registry.getVaultIntegrationState().isEconomyClassesAvailable()) {
            return false;
        }
        try {
            Class.forName("net.milkbowl.vault.economy.Economy", false, services.getClassLoader());
            return true;
        }
        catch (ClassNotFoundException | LinkageError exception) {
            registry.getVaultIntegrationState().setEconomyClassesAvailable(false);
            return false;
        }
    }
}
