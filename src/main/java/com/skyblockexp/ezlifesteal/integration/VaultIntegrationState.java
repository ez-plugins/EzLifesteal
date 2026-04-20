package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import net.milkbowl.vault.economy.Economy;

public final class VaultIntegrationState {

    private Economy economy;

    private boolean economyClassesAvailable = true;

    private VaultServiceListener serviceListener;

    public Economy getEconomy() {
        return economy;
    }

    public void setEconomy(Economy economy) {
        this.economy = economy;
    }

    public boolean isEconomyClassesAvailable() {
        return economyClassesAvailable;
    }

    public void setEconomyClassesAvailable(boolean economyClassesAvailable) {
        this.economyClassesAvailable = economyClassesAvailable;
    }

    public VaultServiceListener getServiceListener() {
        return serviceListener;
    }

    public void setServiceListener(VaultServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }
}
