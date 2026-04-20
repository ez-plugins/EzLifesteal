package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import net.milkbowl.vault.economy.Economy;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class VaultIntegrationStateTest {

    @Test
    void gettersAndSettersWork() {
        VaultIntegrationState state = new VaultIntegrationState();

        assertNull(state.getEconomy());
        assertTrue(state.isEconomyClassesAvailable());
        assertNull(state.getServiceListener());

        Economy econ = mock(Economy.class);
        VaultServiceListener listener = mock(VaultServiceListener.class);

        state.setEconomy(econ);
        state.setEconomyClassesAvailable(false);
        state.setServiceListener(listener);

        assertSame(econ, state.getEconomy());
        assertFalse(state.isEconomyClassesAvailable());
        assertSame(listener, state.getServiceListener());
    }

    @Test
    void economyProviderStateCanToggleBetweenPresentAndAbsent() {
        VaultIntegrationState state = new VaultIntegrationState();
        Economy provider = mock(Economy.class);

        state.setEconomy(provider);
        assertSame(provider, state.getEconomy());

        state.setEconomy(null);
        assertNull(state.getEconomy());
    }

    @Test
    void economyClassAvailabilityFlagCanToggle() {
        VaultIntegrationState state = new VaultIntegrationState();

        assertTrue(state.isEconomyClassesAvailable());

        state.setEconomyClassesAvailable(false);
        assertFalse(state.isEconomyClassesAvailable());

        state.setEconomyClassesAvailable(true);
        assertTrue(state.isEconomyClassesAvailable());
    }

    @Test
    void serviceListenerReferenceCanBeStoredAndCleared() {
        VaultIntegrationState state = new VaultIntegrationState();
        VaultServiceListener serviceListener = mock(VaultServiceListener.class);

        state.setServiceListener(serviceListener);
        assertSame(serviceListener, state.getServiceListener());

        state.setServiceListener(null);
        assertNull(state.getServiceListener());
    }

    @Test
    void providerReferenceCanDisappearMidOperationWithoutCorruptingCachedSnapshot() {
        VaultIntegrationState state = new VaultIntegrationState();
        Economy provider = mock(Economy.class);
        state.setEconomy(provider);

        java.util.concurrent.atomic.AtomicReference<Economy> operationSnapshot =
                new java.util.concurrent.atomic.AtomicReference<>();
        operationSnapshot.set(state.getEconomy());
        state.setEconomy(null);

        assertSame(provider, operationSnapshot.get());
        assertNull(state.getEconomy());
    }
}
