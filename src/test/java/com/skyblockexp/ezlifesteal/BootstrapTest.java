package com.skyblockexp.ezlifesteal;

import com.skyblockexp.ezlifesteal.runtime.Bootstrap;
import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import com.skyblockexp.ezlifesteal.runtime.PluginAccessor;
import com.skyblockexp.ezlifesteal.runtime.PluginRuntimeCoordinator;
import com.skyblockexp.ezlifesteal.runtime.Registry;
import java.lang.reflect.Field;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class BootstrapTest {

    @Test
    void startSetsBootstrapAndInitializesCoordinator() throws Exception {
        Bootstrap bootstrap = bootstrapWithCoordinatorMock();
        Registry registry = getField(bootstrap, "registry", Registry.class);
        PluginRuntimeCoordinator coordinator = getField(bootstrap, "coordinator", PluginRuntimeCoordinator.class);

        bootstrap.start();

        InOrder order = inOrder(registry, coordinator);
        order.verify(registry).setBootstrap(bootstrap);
        order.verify(coordinator).initializePlugin();
    }

    @Test
    void stopShutsDownCoordinatorThenClearsBootstrap() throws Exception {
        Bootstrap bootstrap = bootstrapWithCoordinatorMock();
        Registry registry = getField(bootstrap, "registry", Registry.class);
        PluginRuntimeCoordinator coordinator = getField(bootstrap, "coordinator", PluginRuntimeCoordinator.class);

        bootstrap.stop();

        InOrder order = inOrder(coordinator, registry);
        order.verify(coordinator).shutdownPlugin();
        order.verify(registry).setBootstrap(null);
    }

    @Test
    void reloadDelegatesToCoordinatorWithInitiator() throws Exception {
        Bootstrap bootstrap = bootstrapWithCoordinatorMock();
        PluginRuntimeCoordinator coordinator = getField(bootstrap, "coordinator", PluginRuntimeCoordinator.class);
        CommandSender initiator = mock(CommandSender.class);

        bootstrap.reload(initiator);

        verify(coordinator).reloadPlugin(initiator);
    }

    @Test
    void gettersReturnCoordinatorProvidedValues() throws Exception {
        Bootstrap bootstrap = bootstrapWithCoordinatorMock();
        PluginRuntimeCoordinator coordinator = getField(bootstrap, "coordinator", PluginRuntimeCoordinator.class);
        PluginAccessor accessor = mock(PluginAccessor.class);
        DefaultPluginRuntimeServices services = mock(DefaultPluginRuntimeServices.class);

        org.mockito.Mockito.when(coordinator.getPluginAccessor()).thenReturn(accessor);
        org.mockito.Mockito.when(coordinator.getRuntimeServices()).thenReturn(services);

        assertSame(accessor, bootstrap.getPluginAccessor());
        assertSame(services, bootstrap.getRuntimeServices());
    }

    private static Bootstrap bootstrapWithCoordinatorMock() throws Exception {
        EzLifestealPlugin plugin = mock(EzLifestealPlugin.class);
        Registry registry = mock(Registry.class);
        Bootstrap bootstrap = new Bootstrap(plugin, registry);
        setField(bootstrap, "coordinator", mock(PluginRuntimeCoordinator.class));
        return bootstrap;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T getField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
