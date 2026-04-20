package com.skyblockexp.ezlifesteal.config;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class ConfigRuntimeServiceTest {

    @Test
    void startInvokesRuntimeMethodsInExpectedOrder() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        ConfigRuntimeService service = new ConfigRuntimeService(runtime);

        service.start();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).saveDefaultConfig();
        inOrder.verify(runtime).ensureAdditionalConfigFiles();
        inOrder.verify(runtime).reloadAdditionalConfigs();
        inOrder.verify(runtime).setupMessages();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void reloadInvokesRuntimeMethodsInExpectedOrder() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        ConfigRuntimeService service = new ConfigRuntimeService(runtime);

        service.reload();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).reloadConfig();
        inOrder.verify(runtime).reloadAdditionalConfigs();
        inOrder.verify(runtime).setupMessages();
        inOrder.verifyNoMoreInteractions();
    }
}
