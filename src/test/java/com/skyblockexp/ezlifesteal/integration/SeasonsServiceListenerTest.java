package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;
import com.skyblockexp.ezlifesteal.util.SchedulerAdapter;
import com.skyblockexp.lifesteal.seasons.api.SeasonsApi;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class SeasonsServiceListenerTest {

    @Test
    void registerAndUnregisterEventsForSeasonsApiRunTheirHooksExactlyOncePerEvent() {
        Plugin plugin = mock(Plugin.class);
        AtomicInteger hookCalls = new AtomicInteger();
        AtomicInteger unhookCalls = new AtomicInteger();
        SeasonsServiceListener listener = new SeasonsServiceListener(plugin, hookCalls::incrementAndGet,
                unhookCalls::incrementAndGet);

        ServiceRegisterEvent registerEvent = registerEventFor(SeasonsApi.class);
        ServiceUnregisterEvent unregisterEvent = unregisterEventFor(SeasonsApi.class);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(Plugin.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable action = invocation.getArgument(1);
                        action.run();
                        return null;
                    });

            listener.onServiceRegister(registerEvent);
            listener.onServiceUnregister(unregisterEvent);

            assertEquals(1, hookCalls.get());
            assertEquals(1, unhookCalls.get());
        }
    }

    @Test
    void nonSeasonsServicesDoNotTriggerHooks() {
        Plugin plugin = mock(Plugin.class);
        AtomicInteger hookCalls = new AtomicInteger();
        AtomicInteger unhookCalls = new AtomicInteger();
        SeasonsServiceListener listener = new SeasonsServiceListener(plugin, hookCalls::incrementAndGet,
                unhookCalls::incrementAndGet);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            listener.onServiceRegister(registerEventFor(String.class));
            listener.onServiceUnregister(unregisterEventFor(String.class));

            scheduler.verifyNoInteractions();
            assertEquals(0, hookCalls.get());
            assertEquals(0, unhookCalls.get());
        }
    }

    @Test
    void duplicateEventsDoNotCauseDuplicateStateTransitions() {
        Plugin plugin = mock(Plugin.class);
        AtomicBoolean hooked = new AtomicBoolean(false);
        AtomicInteger hookTransitions = new AtomicInteger();
        AtomicInteger unhookTransitions = new AtomicInteger();

        SeasonsServiceListener listener = new SeasonsServiceListener(
                plugin,
                () -> {
                    if (hooked.compareAndSet(false, true)) {
                        hookTransitions.incrementAndGet();
                    }
                },
                () -> {
                    if (hooked.compareAndSet(true, false)) {
                        unhookTransitions.incrementAndGet();
                    }
                }
        );

        ServiceRegisterEvent registerEvent = registerEventFor(SeasonsApi.class);
        ServiceUnregisterEvent unregisterEvent = unregisterEventFor(SeasonsApi.class);

        try (MockedStatic<SchedulerAdapter> scheduler = mockStatic(SchedulerAdapter.class)) {
            scheduler.when(() -> SchedulerAdapter.run(any(Plugin.class), any(Runnable.class)))
                    .thenAnswer(invocation -> {
                        Runnable action = invocation.getArgument(1);
                        action.run();
                        return null;
                    });

            listener.onServiceRegister(registerEvent);
            listener.onServiceRegister(registerEvent);
            assertTrue(hooked.get());
            assertEquals(1, hookTransitions.get());

            listener.onServiceUnregister(unregisterEvent);
            listener.onServiceUnregister(unregisterEvent);
            assertFalse(hooked.get());
            assertEquals(1, unhookTransitions.get());
        }
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
