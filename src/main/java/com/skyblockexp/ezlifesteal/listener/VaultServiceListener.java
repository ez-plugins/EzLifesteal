package com.skyblockexp.ezlifesteal.listener;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;

public final class VaultServiceListener implements Listener {

    private final BooleanSupplier vaultClassesAvailable;

    private final Runnable vaultRefreshAction;


    public VaultServiceListener(BooleanSupplier vaultClassesAvailable, Runnable vaultRefreshAction) {
        this.vaultClassesAvailable = Objects.requireNonNull(vaultClassesAvailable, "vaultClassesAvailable");
        this.vaultRefreshAction = Objects.requireNonNull(vaultRefreshAction, "vaultRefreshAction");
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent event) {
        if (!vaultClassesAvailable.getAsBoolean()) {
            return;
        }
        if (Economy.class.isAssignableFrom(event.getProvider().getService())) {
            vaultRefreshAction.run();
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent event) {
        if (!vaultClassesAvailable.getAsBoolean()) {
            return;
        }
        if (Economy.class.isAssignableFrom(event.getProvider().getService())) {
            vaultRefreshAction.run();
        }
    }
}
