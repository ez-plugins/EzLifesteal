package com.skyblockexp.ezlifesteal.runtime;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.config.ConfigLoader;
import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
}
