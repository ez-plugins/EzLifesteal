package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class SeasonsIntegrationStateTest {

    @Test
    void clearIntegrationRegistrationClearsOnlyApiAndProxy() {
        SeasonsIntegrationState state = new SeasonsIntegrationState();

        Class<?> apiClass = String.class;
        Class<?> integrationClass = Runnable.class;
        Class<?> profileClass = Integer.class;
        Object apiInstance = new Object();
        Object integrationProxy = new Object();

        state.setApiClass(apiClass);
        state.setIntegrationClass(integrationClass);
        state.setProfileClass(profileClass);
        state.setApiInstance(apiInstance);
        state.setIntegrationProxy(integrationProxy);

        state.clearIntegrationRegistration();

        assertNull(state.getApiInstance());
        assertNull(state.getIntegrationProxy());
        assertSame(apiClass, state.getApiClass());
        assertSame(integrationClass, state.getIntegrationClass());
        assertSame(profileClass, state.getProfileClass());
    }

    @Test
    void clearLoadedClassesClearsOnlyCachedClassReferences() {
        SeasonsIntegrationState state = new SeasonsIntegrationState();

        Object apiInstance = new Object();
        Object integrationProxy = new Object();
        state.setApiClass(String.class);
        state.setIntegrationClass(Runnable.class);
        state.setProfileClass(Integer.class);
        state.setApiInstance(apiInstance);
        state.setIntegrationProxy(integrationProxy);

        state.clearLoadedClasses();

        assertNull(state.getApiClass());
        assertNull(state.getIntegrationClass());
        assertNull(state.getProfileClass());
        assertSame(apiInstance, state.getApiInstance());
        assertSame(integrationProxy, state.getIntegrationProxy());
    }

    @Test
    void listenerReferencesCanBeStoredAndCleared() {
        SeasonsIntegrationState state = new SeasonsIntegrationState();

        Plugin plugin = mock(Plugin.class);
        SeasonsServiceListener serviceListener = new SeasonsServiceListener(plugin, () -> { }, () -> { });
        SeasonResetListener seasonResetListener = mock(SeasonResetListener.class);

        state.setServiceListener(serviceListener);
        state.setSeasonResetListener(seasonResetListener);

        assertSame(serviceListener, state.getServiceListener());
        assertSame(seasonResetListener, state.getSeasonResetListener());

        state.setServiceListener(null);
        state.setSeasonResetListener(null);

        assertNull(state.getServiceListener());
        assertNull(state.getSeasonResetListener());
    }
}
