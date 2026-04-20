package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.VaultServiceListener;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

class VaultServiceListenerTest {

    @Test
    void registerAndUnregisterEconomyEventsTriggerRefreshExactlyOncePerEvent() {
        AtomicInteger refreshCalls = new AtomicInteger();
        VaultServiceListener listener = new VaultServiceListener(() -> true, refreshCalls::incrementAndGet);

        listener.onServiceRegister(registerEventFor(Economy.class));
        listener.onServiceUnregister(unregisterEventFor(Economy.class));

        assertEquals(2, refreshCalls.get());
    }

    @Test
    void ignoresEventsWhenVaultEconomyClassesUnavailableOrServiceIsNotEconomy() {
        AtomicInteger refreshCalls = new AtomicInteger();
        VaultServiceListener unavailableListener = new VaultServiceListener(() -> false, refreshCalls::incrementAndGet);
        unavailableListener.onServiceRegister(registerEventFor(Economy.class));
        unavailableListener.onServiceUnregister(unregisterEventFor(Economy.class));

        VaultServiceListener wrongServiceListener = new VaultServiceListener(() -> true, refreshCalls::incrementAndGet);
        wrongServiceListener.onServiceRegister(registerEventFor(String.class));
        wrongServiceListener.onServiceUnregister(unregisterEventFor(String.class));

        assertEquals(0, refreshCalls.get());
    }

    @Test
    void duplicateEventsDoNotCauseDuplicateStateTransitions() {
        AtomicBoolean providerAvailable = new AtomicBoolean(false);
        AtomicBoolean runtimeCachedState = new AtomicBoolean(false);
        AtomicInteger refreshTransitions = new AtomicInteger();

        VaultServiceListener listener = new VaultServiceListener(
                () -> true,
                () -> {
                    boolean currentAvailability = providerAvailable.get();
                    boolean previousAvailability = runtimeCachedState.getAndSet(currentAvailability);
                    if (previousAvailability != currentAvailability) {
                        refreshTransitions.incrementAndGet();
                    }
                }
        );

        providerAvailable.set(true);
        listener.onServiceRegister(registerEventFor(Economy.class));
        listener.onServiceRegister(registerEventFor(Economy.class));
        assertTrue(runtimeCachedState.get());
        assertEquals(1, refreshTransitions.get());

        providerAvailable.set(false);
        listener.onServiceUnregister(unregisterEventFor(Economy.class));
        listener.onServiceUnregister(unregisterEventFor(Economy.class));
        assertFalse(runtimeCachedState.get());
        assertEquals(2, refreshTransitions.get());
    }

    private ServiceRegisterEvent registerEventFor(Class<?> serviceClass) {
        RegisteredServiceProvider<?> provider = mock(RegisteredServiceProvider.class);
        doReturn(serviceClass).when(provider).getService();
        ServiceRegisterEvent event = mock(ServiceRegisterEvent.class);
        doReturn(provider).when(event).getProvider();
        return event;
    }

    private ServiceUnregisterEvent unregisterEventFor(Class<?> serviceClass) {
        RegisteredServiceProvider<?> provider = mock(RegisteredServiceProvider.class);
        doReturn(serviceClass).when(provider).getService();
        ServiceUnregisterEvent event = mock(ServiceUnregisterEvent.class);
        doReturn(provider).when(event).getProvider();
        return event;
    }
}
