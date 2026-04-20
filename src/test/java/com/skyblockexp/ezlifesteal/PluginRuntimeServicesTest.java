package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.integration.VaultIntegrationState;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DefaultPluginRuntimeServicesTest {

    @Test
    void sanitizeHeartBounds_noAdjustment() {
        DefaultPluginRuntimeServices.SanitizedHeartBounds bounds = DefaultPluginRuntimeServices.sanitizeHeartBounds(1.0,
                5.0, 10.0);
        assertFalse(bounds.adjusted());
        assertEquals(1.0, bounds.minHearts());
        assertEquals(5.0, bounds.defaultHearts());
        assertEquals(10.0, bounds.maxHearts());
    }

    @Test
    void sanitizeHeartBounds_adjusts_negativeMinAndDefaultAboveMax() {
        DefaultPluginRuntimeServices.SanitizedHeartBounds bounds =
                DefaultPluginRuntimeServices.sanitizeHeartBounds(-5.0,
                50.0, 10.0);
        assertTrue(bounds.adjusted());
        assertEquals(0.0, bounds.minHearts());
        assertEquals(10.0, bounds.defaultHearts());
        assertEquals(10.0, bounds.maxHearts());
    }

    @Test
    void ensureVaultEconomyClassesAvailable_setsFlagWhenMissing() {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = new Registry();
        // ensure the flag begins true so the method attempts a Class.forName
        VaultIntegrationState vaultState = registry.getVaultIntegrationState();
        vaultState.setEconomyClassesAvailable(true);

        DefaultPluginRuntimeServices services = new DefaultPluginRuntimeServices(plugin, registry);

        // Ensure method result matches registry flag state after invocation
        boolean available = services.ensureVaultEconomyClassesAvailable();
        assertEquals(available, registry.getVaultIntegrationState().isEconomyClassesAvailable());
    }
}
